package com.dbio.protocol

import cats.data.ReaderT
import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import ironoxide.v1.IronOxide
import ironoxide.v1.common.UserId
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._

import java.time.ZonedDateTime

/** Packages necessary client dependencies as one object. */
final case class InjectClients(
  iron: IronOxide[IO],
  client: Client[IO]
)

/** A registered user in the dBio system. */
final case class User(
  ethPublicAddress: String,
  email: String
)

object User {

  implicit val userDecoder: Decoder[User] =
    Decoder.forProduct2("eth_public_address", "email")(User.apply)

  implicit val userEntityDecoder: EntityDecoder[IO, User] =
    jsonOf[IO, User]

}

/** Simple type required in body of GET requests for FHIR Resources. */
final case class Requestor(ethAddress: String)

object Requestor {
  implicit val reqEnc: Encoder[Requestor] =
    Encoder.forProduct1("requestor_eth_address")(a => (a.ethAddress))
  implicit val reqEntEnc: EntityEncoder[IO, Requestor] = jsonEncoderOf[IO, Requestor]
}

/** Request constructed by caller to POST a Resource to the dBio protocol.
  */
final case class DbioPostRequest(
  subjectEmail: String,
  creatorEmail: String,
  creatorEthAddress: String,
  fhirResourceType: String,
  fhirResourceId: String,
  plaintext: String
)

/** Response from dBio protocol after posting a Resource.
  */
final case class DbioPostResponse(
  fhirResourceId: String,
  ironcoreDocumentId: String,
  subjectEthAddress: String,
  creatorEthAddress: String,
  fhirResourceType: String,
  ciphertext: String,
  timestamp: ZonedDateTime
)

object DbioPostResponse {

  implicit val postDecoder: Decoder[DbioPostResponse] =
    Decoder.forProduct7(
      "fhir_resource_id",
      "ironcore_document_id",
      "subject_eth_address",
      "creator_eth_address",
      "fhir_resource_type",
      "ciphertext",
      "timestamp")(DbioPostResponse.apply)

  implicit val decodeResponse: EntityDecoder[IO, DbioPostResponse] =
    jsonOf[IO, DbioPostResponse]

}

/** Intermediate payload type used by this module -- does not need to be constructed by caller.
  */
final case class PostPayload(
  email: String,
  creatorEthAddress: String,
  fhirResourceType: String,
  fhirResourceId: String,
  ironcoreDocumentId: String,
  ciphertext: String
)

object PostPayload {

  implicit val postEncoder: Encoder[PostPayload] =
    new Encoder[PostPayload] {
      def apply(a: PostPayload): Json = Json.obj(
        "email" -> Json.fromString(a.email),
        "creator_eth_address" -> Json.fromString(a.creatorEthAddress),
        "fhir_resource_type" -> Json.fromString(a.fhirResourceType),
        "fhir_resource_id" -> Json.fromString(a.fhirResourceId),
        "ironcore_document_id" -> Json.fromString(a.ironcoreDocumentId),
        "ciphertext" -> Json.fromString(a.ciphertext)
      )
    }

  implicit val postEntityEncoder: EntityEncoder[IO, PostPayload] =
    jsonEncoderOf[IO, PostPayload]

}

/** Request object to be constructed when querying dBio protocol for a Resource.
  */
final case class DbioGetRequest(
  requesteeEmail: String,
  requestorEmail: String,
  resourceType: String,
  resourceId: String
)

/** Response from protocol server containing a resource in plaintext and metadata.
  */
final case class DbioGetResponse(
  resource: DbioResource,
  plaintext: Json
)

/** Persisted ciphertext from IPFS containing JSON of a FHIR resource.
  */
final case class DbioResource(
  cid: Option[String],
  ciphertext: String,
  ironcoreDocumentId: String,
  fhirResourceId: String,
  fhirResourceType: String
)

object DbioResource {

  implicit val resourceDecoder: Decoder[DbioResource] =
    Decoder.forProduct5(
      "cid",
      "ciphertext",
      "ironcore_document_id",
      "fhir_resource_id",
      "fhir_resource_type")(DbioResource.apply)

  implicit val resourceEntityDecoder: EntityDecoder[IO, DbioResource] = jsonOf[IO, DbioResource]

  val allocateClient: IO[(Client[IO], IO[Unit])] = BlazeClientBuilder[IO].allocated
  private val Base: Uri = uri"http://dbio-protocol:8080/dbio"
  private val ResourcesClaimed: Uri = Base / "resources" / "claimed"
  private val ResourcesUnclaimed: Uri = Base / "resources" / "unclaimed"
  private val UserByEmail: Uri = Base / "users" / "email"

  /** Get a User's information given their email address. */
  private def getUser(email: String): ReaderT[IO, Client[IO], User] =
    ReaderT { client =>
      client.expect[User](UserByEmail / email)
    }

  private def resourceRequest(
    user: User,
    requestor: Requestor,
    req: DbioGetRequest,
    uri: Uri
  ): Request[IO] = {
    val route =
      uri / user.ethPublicAddress / req.resourceType / req.resourceId / requestor.ethAddress
    Request[IO](method = GET, uri = route)
  }

  /** Reads a ciphertext resource from the backend and decrypts it. */
  def get(req: DbioGetRequest): ReaderT[IO, InjectClients, DbioGetResponse] =
    ReaderT { case InjectClients(iron, client) =>
      for {
        subject <- getUser(req.requesteeEmail).run(client)
        _requestor <- getUser(req.requestorEmail).run(client)
        requestor = Requestor(_requestor.ethPublicAddress)
        claimed = resourceRequest(subject, requestor, req, ResourcesClaimed)
        unclaimed = resourceRequest(subject, requestor, req, ResourcesUnclaimed)
        resource <- client.fetch[DbioResource](claimed) { response =>
          if (response.status.isSuccess) response.as[DbioResource]
          else client.expect[DbioResource](unclaimed)
        }
        plaintext <- IronCore.decrypt(resource.ciphertext).run(iron)
      } yield DbioGetResponse(resource, plaintext)
    }

  /** Posts given plaintext to dBio backend as an encrypted and unclaimed resource. Encrypts
    * plaintext to a transfer group between this third party (creator) and intended user (subject).
    */
  def post(req: DbioPostRequest): ReaderT[IO, InjectClients, DbioPostResponse] =
    ReaderT { case InjectClients(iron, client) =>
      val payload = for {
        json <- ReaderT.liftF(IO.fromEither(parse(req.plaintext)))
        result <- IronCore.transferEncrypt(
          json,
          from = UserId(req.creatorEmail),
          to = UserId(req.subjectEmail))
      } yield PostPayload(
        email = req.subjectEmail,
        creatorEthAddress = req.creatorEthAddress,
        fhirResourceType = req.fhirResourceType,
        fhirResourceId = req.fhirResourceId,
        ironcoreDocumentId = result.id.id,
        ciphertext = result.encryptedData.toBase64
      )
      for {
        body <- payload.run(iron)
        _ <- IO.println(s"[DbioResource] POST JSON payload: ${body.asJson.noSpaces}")
        req = Request[IO](method = POST, uri = ResourcesUnclaimed).withEntity(body)
        out <- client.expect[DbioPostResponse](req)
      } yield out
    }

}

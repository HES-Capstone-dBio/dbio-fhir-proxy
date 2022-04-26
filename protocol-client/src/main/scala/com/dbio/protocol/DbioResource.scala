package com.dbio.protocol

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import ironoxide.v1.common.UserId
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._

import java.time.ZonedDateTime

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

/** Request constructed by caller to POST a Resource to the dBio protocol.
  */
final case class DbioPostRequest(
  subjectEmail: String,
  creatorEmail: String,
  password: String,
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
  password: String,
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

  private val clientR: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].resource
  private val Base: Uri = uri"http://dbio-protocol:8080/dbio"
  private val ResourcesClaimed: Uri = Base / "resources" / "claimed"
  private val ResourcesUnclaimed: Uri = Base / "resources" / "unclaimed"
  private val UserByEmail: Uri = Base / "users" / "email"

  /** Get a User's information given their email address. */
  private def getUser(email: String): ReaderT[IO, Client[IO], User] =
    ReaderT { client =>
      client.expect[User](UserByEmail / email)
    }

  /** Reads a ciphertext resource from the backend and decrypts it. */
  def get(req: DbioGetRequest): IO[DbioGetResponse] =
    clientR use { client =>
      for {
        user <- getUser(req.requesteeEmail).run(client)
        claimed = ResourcesClaimed / user.ethPublicAddress / req.resourceType / req.resourceId
        unclaimed = ResourcesUnclaimed / user.ethPublicAddress / req.resourceType / req.resourceId
        resource <- client.get[DbioResource](claimed) { response =>
          if (response.status.isSuccess) response.as[DbioResource]
          else client.expect[DbioResource](unclaimed)
        }
        iron <- IronCore.forUser(req.requestorEmail, req.password)
        plaintext <- IronCore.decrypt(resource.ciphertext).run(iron)
      } yield DbioGetResponse(resource, plaintext)
    }

  /** Posts given plaintext to dBio backend as an encrypted and unclaimed resource. Encrypts
    * plaintext to a transfer group between this third party (creator) and intended user (subject).
    */
  def post(req: DbioPostRequest): IO[DbioPostResponse] =
    clientR use { client =>
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
        iron <- IronCore.forUser(req.creatorEmail, req.password)
        body <- payload.run(iron)
        _ <- IO.println(s"[DbioResource] POST JSON payload: ${body.asJson.noSpaces}")
        req = Request[IO](method = POST, uri = ResourcesUnclaimed).withEntity(body)
        out <- client.expect[DbioPostResponse](req)
      } yield out
    }

}

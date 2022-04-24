package com.dbio.protocol

import cats.data.ReaderT
import cats.effect.IO
import cats.effect.kernel.Resource
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import ironoxide.v1.common.UserId
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import io.circe.syntax._

import java.time.ZonedDateTime
import io.circe.Encoder
import io.circe.Decoder
import io.circe.HCursor

final case class User(
  ethPublicAddress: String,
  email: String
)

object User {
  implicit val userDecoder: EntityDecoder[IO, User] =
    jsonOf[IO, User]
}

final case class DbioPostRequest(
  subjectEmail: String,
  creatorEmail: String,
  password: String,
  creatorEthAddress: String,
  fhirResourceType: String,
  fhirResourceId: String,
  plaintext: String
)

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
    new Decoder[DbioPostResponse] {
      def apply(c: HCursor): Decoder.Result[DbioPostResponse] =
        for {
          fhirResourceId <- c.downField("fhir_resource_id").as[String]
          ironcoreDocumentId <- c.downField("ironcore_document_id").as[String]
          subjectEthAddress <- c.downField("subject_eth_address").as[String]
          creatorEthAddress <- c.downField("creator_eth_address").as[String]
          fhirResourceType <- c.downField("fhir_resource_type").as[String]
          ciphertext <- c.downField("ciphertext").as[String]
          timestamp <- c.downField("timestamp").as[ZonedDateTime]
        } yield DbioPostResponse(
          fhirResourceId,
          ironcoreDocumentId,
          subjectEthAddress,
          creatorEthAddress,
          fhirResourceType,
          ciphertext,
          timestamp)
    }

  implicit val decodeResponse: EntityDecoder[IO, DbioPostResponse] =
    jsonOf[IO, DbioPostResponse]

}

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

final case class DbioGetRequest(
  requesteeEmail: String,
  requestorEmail: String,
  password: String,
  resourceType: String,
  resourceId: String
)

final case class DbioGetResponse(
  resource: DbioResource,
  plaintext: Json
)

/** Persisted ciphertext from IPFS containing JSON of a FHIR resource.
  */
final case class DbioResource(
  cid: String,
  ciphertext: String,
  ironcoreDocumentId: String,
  fhirResourceId: String,
  fhirResourceType: String
)

object DbioResource {
  private val clientR: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].resource
  implicit val resourceDecoder: EntityDecoder[IO, DbioResource] = jsonOf[IO, DbioResource]
  private val Base: Uri = uri"http://dbio-protocol_dbio-protocol_1:8080/dbio"
  private val ResourcesClaimed: Uri = Base / "resources" / "claimed"
  private val ResourcesUnclaimed: Uri = Base / "resources" / "unclaimed"
  private val UserByEmail: Uri = Base / "users" / "email"

  private def getUser(email: String): ReaderT[IO, Client[IO], User] =
    ReaderT { client =>
      client.expect[User](UserByEmail / email)
    }

  /** Reads a ciphertext resource from the backend and decrypts it.
    */
  def get(req: DbioGetRequest): IO[DbioGetResponse] =
    clientR use { client =>
      for {
        user <- getUser(req.requesteeEmail).run(client)
        at = ResourcesClaimed / user.ethPublicAddress / req.resourceType / req.resourceId
        resource <- client.expect[DbioResource](at)
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
        ciphertext = result.encryptedData.toBin
      )
      for {
        _ <- IO.println(s"[DbioResource] POST $req")
        iron <- IronCore.forUser(req.creatorEmail, req.password)
        body <- payload.run(iron)
        _ <- IO.println(s"[DbioResource] POST JSON payload: ${body.asJson.noSpaces}")
        req = Request[IO](method = POST, uri = ResourcesUnclaimed).withEntity(body)
        out <- client.expect[DbioPostResponse](req)
        _ <- IO.println(s"[DbioResource] $out")
      } yield out

    }

}

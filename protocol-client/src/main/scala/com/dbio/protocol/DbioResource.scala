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

import java.time.ZonedDateTime

final case class User(
  ethPublicAddress: String,
  email: String
)

object User {
  implicit val userDecoder: EntityDecoder[IO, User] =
    jsonOf[IO, User]
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
  implicit val postEncoder: EntityEncoder[IO, PostPayload] =
    jsonEncoderOf[IO, PostPayload]
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
  implicit val decodeResponse: EntityDecoder[IO, DbioPostResponse] =
    jsonOf[IO, DbioPostResponse]
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
  private val Base: Uri = uri"https://dbio-protocol:8080/dbio"
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
        iron <- IronCore.forUser(req.creatorEmail, req.password)
        body <- payload.run(iron)
        req = Request[IO](method = POST, uri = ResourcesUnclaimed).withEntity(body)
        out <- client.expect[DbioPostResponse](req)
      } yield out

    }

}

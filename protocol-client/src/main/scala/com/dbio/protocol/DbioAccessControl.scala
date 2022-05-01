package com.dbio.protocol

import cats.data.ReaderT
import cats.effect.IO
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.http4s.Method.POST
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{Uri, _}

import java.time.ZonedDateTime

final case class AccessRequest(
  requestorEthAddress: String,
  requesteeEthAddress: String,
  requestorDetails: String
)

object AccessRequest {
  implicit val decAR: Decoder[AccessRequest] =
    Decoder.forProduct3("requestor_eth_address", "requestee_eth_address", "requestor_details")(
      AccessRequest.apply)

  implicit val encAR: Encoder[AccessRequest] =
    Encoder.forProduct3("requestor_eth_address", "requestee_eth_address", "requestor_details")(a =>
      (a.requestorEthAddress, a.requesteeEthAddress, a.requestorDetails))

  implicit val entDecAR: EntityDecoder[IO, AccessRequest] =
    jsonOf[IO, AccessRequest]

  implicit val entEncAR: EntityEncoder[IO, AccessRequest] =
    jsonEncoderOf[IO, AccessRequest]
}

final case class AccessRequestStatus(
  id: Int,
  requestorEthAddress: String,
  requesteeEthAddress: String,
  requestApproved: Boolean,
  requestOpen: Boolean,
  createdDate: ZonedDateTime,
  lastUpdatedDate: ZonedDateTime
)

object AccessRequestStatus {
  implicit val decARS: Decoder[AccessRequestStatus] =
    Decoder.forProduct7(
      "id",
      "requestor_eth_address",
      "requestee_eth_address",
      "request_approved",
      "request_open",
      "created_date",
      "last_updated_date")(AccessRequestStatus.apply)

  implicit val entDecARS: EntityDecoder[IO, AccessRequestStatus] =
    jsonOf[IO, AccessRequestStatus]

  implicit val entDecARSList: EntityDecoder[IO, List[AccessRequestStatus]] =
    jsonOf[IO, List[AccessRequestStatus]]
}

object DbioAccessControl {
  val Base: Uri = uri"http://dbio-protocol:8080/dbio"
  val ReadRequests = Base / "read_requests"
  val WriteRequests = Base / "write_requests"

  private def doPost(ar: AccessRequest, to: Uri): ReaderT[IO, Client[IO], AccessRequestStatus] = {
    val req = Request[IO](method = POST, uri = to).withEntity(ar)
    ReaderT(client => client.fetchAs[AccessRequestStatus](req))
  }

  /** Posts a DbioReadRequest for a given user. */
  def postReadRequest(ar: AccessRequest, client: Client[IO]): IO[AccessRequestStatus] =
    doPost(ar, ReadRequests).run(client)

  /** Posts a DbioWriteRequest for a given user. */
  def postWriteRequest(ar: AccessRequest, client: Client[IO]): IO[AccessRequestStatus] =
    doPost(ar, WriteRequests).run(client)

  private def doGetList(
    requesteeEth: String,
    uri: Uri
  ): ReaderT[IO, Client[IO], List[AccessRequestStatus]] =
    ReaderT(client =>
      client.expect[List[AccessRequestStatus]](
        (uri / requesteeEth).withQueryParam("filter", "open")))

  private def doGet(
    id: Int,
    uri: Uri
  ): ReaderT[IO, Client[IO], AccessRequestStatus] =
    ReaderT(client => client.expect[AccessRequestStatus](uri / "id" / id))

  /** Gets list of DbioReadRequests for the given user. */
  def getReadRequests(requestee: String, client: Client[IO]): IO[List[AccessRequestStatus]] =
    doGetList(requestee, ReadRequests).run(client)

  /** Gets list of DbioWriteRequests for the given user. */
  def getWriteRequests(requestee: String, client: Client[IO]): IO[List[AccessRequestStatus]] =
    doGetList(requestee, WriteRequests).run(client)

  /** Gets single DbioReadRequest for the given user and requestor pair. */
  def getReadRequest(id: Int, requestor: String, client: Client[IO]): IO[AccessRequestStatus] =
    doGet(id, ReadRequests)
      .run(client)
      .flatMap(a =>
        if (a.requestorEthAddress === requestor) IO.pure(a)
        else
          IO.raiseError(
            new IllegalAccessError(
              s"[DbioAccessControl] AccessRequest not for requestor=$requestor")))

  /** Gets single DbioWriteRequests for the given user and requestor pair. */
  def getWriteRequest(id: Int, requestor: String, client: Client[IO]): IO[AccessRequestStatus] =
    doGet(id, WriteRequests)
      .run(client)
      .flatMap(a =>
        if (a.requestorEthAddress === requestor) IO.pure(a)
        else
          IO.raiseError(
            new IllegalAccessError(
              s"[DbioAccessControl] AccessRequest not for requestor=$requestor")))

}

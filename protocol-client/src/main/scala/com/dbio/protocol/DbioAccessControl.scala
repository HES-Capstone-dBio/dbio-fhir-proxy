package com.dbio.protocol

import org.http4s.Uri
import org.http4s.implicits._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.circe._
import cats.data.ReaderT
import cats.effect.IO
import org.http4s.client.Client
import io.circe.Decoder
import org.http4s._
import org.http4s.Method.POST
import io.circe.Encoder

final case class AccessRequest(
  requestorEthAddress: String,
  requesteeEthAddress: String
  // requestorDetails: String,
)

object AccessRequest {
  implicit val decAR: Decoder[AccessRequest] =
    Decoder.forProduct2("requestor_eth_address", "requestee_eth_address")(AccessRequest.apply)

  implicit val encAR: Encoder[AccessRequest] =
    Encoder.forProduct2("requestor_eth_address", "requestee_eth_address")(a =>
      (a.requestorEthAddress, a.requesteeEthAddress))

  implicit val entDecAR: EntityDecoder[IO, AccessRequest] =
    jsonOf[IO, AccessRequest]

  implicit val entEncAR: EntityEncoder[IO, AccessRequest] =
    jsonEncoderOf[IO, AccessRequest]

  // TODO: Switch to this once "details" field is accepted by backend
  // implicit val encAR: Encoder[AccessRequest] =
  // Encoder.forProduct3("requestor_eth_address", "requestee_eth_address", "requestor_details")(a =>
  // (a.requestorEthAddress, a.requesteeEthAddress, a.requestorDetails))
}

final case class AccessRequestStatus(
  id: Int,
  requestorEthAddress: String,
  requesteeEthAddress: String,
  requestApproved: Boolean,
  requestOpen: Boolean
)

object AccessRequestStatus {
  implicit val decARS: Decoder[AccessRequestStatus] =
    Decoder.forProduct5(
      "id",
      "requestor_eth_address",
      "requestee_eth_address",
      "request_approved",
      "request_open")(AccessRequestStatus.apply)

  implicit val entDecARS: EntityDecoder[IO, AccessRequestStatus] =
    jsonOf[IO, AccessRequestStatus]

  implicit val decARSList: Decoder[List[AccessRequestStatus]] =
    Decoder[List[AccessRequestStatus]]

  implicit val entDecARSList: EntityDecoder[IO, List[AccessRequestStatus]] =
    jsonOf[IO, List[AccessRequestStatus]]
}

object DbioAccessControl {
  val Base: Uri = uri"http://dbio-protocol:8080/"
  val ReadRequests = Base / "read_requests"
  val WriteRequests = Base / "write_requests"

  private def doPost(ar: AccessRequest, to: Uri): ReaderT[IO, Client[IO], AccessRequest] = {
    val req = Request[IO](method = POST, uri = to).withEntity(ar)
    ReaderT(client => client.fetchAs[AccessRequest](req))
  }

  /** Posts a DbioReadRequest for a given user. */
  def postReadRequest(ar: AccessRequest, client: Client[IO]): IO[AccessRequest] =
    doPost(ar, ReadRequests).run(client)

  /** Posts a DbioWriteRequest for a given user. */
  def postWriteRequest(ar: AccessRequest, client: Client[IO]): IO[AccessRequest] =
    doPost(ar, WriteRequests).run(client)

  private def doGet(
    requesteeEth: String,
    uri: Uri
  ): ReaderT[IO, Client[IO], List[AccessRequestStatus]] =
    ReaderT(client => client.expect[List[AccessRequestStatus]](uri / requesteeEth))

  /** Gets list of DbioReadRequests for the given user. TODO: Should be queried by requestor not
    * requestee.
    */
  def getReadRequests(requesteeEth: String, client: Client[IO]): IO[List[AccessRequestStatus]] =
    doGet(requesteeEth, ReadRequests).run(client)

  /** Gets list of DbioWriteRequests for the given user. TODO: Should be queried by requestor not
    * requestee.
    */
  def getWriteRequests(requesteeEth: String, client: Client[IO]): IO[List[AccessRequestStatus]] =
    doGet(requesteeEth, WriteRequests).run(client)

}

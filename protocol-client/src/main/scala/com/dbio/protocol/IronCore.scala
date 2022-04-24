package com.dbio.protocol

import cats.data.ReaderT
import cats.effect.IO
import cats.implicits._
import io.circe.Json
import io.circe.parser.parse
import ironoxide.v1.IronOxide
import ironoxide.v1.common.{GroupId, UserId, _}
import ironoxide.v1.document.{DocumentEncryptOpts, DocumentEncryptResult}
import ironoxide.v1.group.GroupCreateOpts
import ironoxide.v1.user.UserCreateOpts
import scodec.bits.ByteVector

import scala.concurrent.duration._

object IronCore {
  private val _ = System.loadLibrary("ironoxide_java")
  private val config = IronOxideConfig(PolicyCachingConfig(666), None)
  private val deviceOpts = DeviceCreateOpts(DeviceName("fhir-proxy"))
  private val timeout = Some(3.seconds)

  /** Authenticate to IronCore for the given user and password. If the user does not exist in the
    * system, creates them.
    *
    * @param user
    *   should be an email address
    * @param pass
    *   the user's password
    */
  def forUser(user: String, pass: String): IO[IronOxide[IO]] =
    for {
      jwt <- JwtSigner.forUser(user)
      verify <- IronOxide.userVerify[IO](jwt, timeout)
      _ <- IO.whenA(verify.isEmpty)(
        IronOxide.userCreate[IO](jwt, pass, UserCreateOpts(), timeout).void)
      device <- IronOxide.generateNewDevice[IO](jwt, pass, deviceOpts, timeout)
      sdk <- IronOxide.initialize[IO](device.toDeviceContext, config)
    } yield sdk

  /** Creates configuration for a "transfer group" between a third party (FHIR proxy) and an
    * existing user in dBio.
    *
    * @param from
    *   third party
    * @param to
    *   patient
    */
  private def transferGroupOpts(from: UserId, to: UserId): GroupCreateOpts =
    GroupCreateOpts(
      id = None,
      name = None,
      addAsAdmin = false,
      addAsMember = true,
      owner = Some(to),
      admins = List(to),
      members = List(from, to),
      needsRotation = false
    )

  /** Transfer options for the incoming resource/document.
    *
    * @param to
    *   intended user recipient of data
    * @param group
    *   transfer group for the user
    */
  private def transferDocumentOpts(to: UserId, group: GroupId): DocumentEncryptOpts =
    DocumentEncryptOpts(
      id = None,
      name = None,
      grantToAuthor = false,
      userGrants = List(to),
      groupGrants = List(group),
      policy = None
    )

  /** Creates a transfer group between third party and existing dBio user. Transfer group should
    * contain escrowed resources from the third party for the user. Once transferred data is claimed
    * by the user, this group can safely be deleted.
    *
    * @param from
    *   third party
    * @param to
    *   existing user (patient)
    * @return
    */
  private def createTransferGroup(from: UserId, to: UserId): ReaderT[IO, IronOxide[IO], GroupId] =
    ReaderT { iron =>
      for {
        jwt <- JwtSigner.forUser(to.id)
        toVerify <- iron.userVerify(jwt, timeout)
        _ <- IO.raiseWhen(toVerify.isEmpty)(
          new IllegalStateException(s"User $to does not exist in IronCore"))
        opts = transferGroupOpts(from, to)
        group <- iron.groupCreate(opts)
      } yield group.id
    }

  /** Encrypts the given resource to a temporary "transfer group" including the target user.
    *
    * @param resource
    *   JSON to encrypt
    * @param from
    *   third party
    * @param to
    *   dbio user for which the resource is intended
    */
  def transferEncrypt(
    resource: Json,
    from: UserId,
    to: UserId
  ): ReaderT[IO, IronOxide[IO], DocumentEncryptResult] =
    for {
      group <- createTransferGroup(from, to)
      doc <- ReaderT { (iron: IronOxide[IO]) =>
        iron.documentEncrypt(
          resource.noSpaces.getBytes(),
          transferDocumentOpts(to, group)
        )
      }
    } yield doc

  /** Decrypts and parses ciphertext data to Json.
    *
    * @param ciphertext
    *   valid byte string of encrypted resource
    */
  def decrypt(ciphertext: String): ReaderT[IO, IronOxide[IO], Json] =
    ReaderT { iron =>
      for {
        bytes <- IO.fromEither(
          ByteVector.fromBase64Descriptive(ciphertext).leftMap(new IllegalArgumentException(_)))
        doc <- iron.documentDecrypt(bytes)
        utf <- IO.fromEither(doc.decryptedData.decodeUtf8)
        json <- IO.fromEither(parse(utf))
      } yield json
    }

}

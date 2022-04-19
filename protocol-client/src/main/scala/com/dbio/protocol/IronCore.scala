package com.dbio.protocol

import cats.effect.IO
import ironoxide.v1.IronOxide
import ironoxide.v1.common._
import ironoxide.v1.user.UserCreateOpts

import scala.concurrent.duration._

object IronCore {

  private val config = IronOxideConfig(PolicyCachingConfig(666), None)
  private val deviceOpts = DeviceCreateOpts(DeviceName("fhir-proxy"))
  private val timeout = Some(3.seconds)

  /**
  * Authenticate to IronCore for the given user and password.
  * If the user does not exist in the system, creates them.
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

}

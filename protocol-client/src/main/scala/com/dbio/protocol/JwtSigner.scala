package com.dbio.protocol

import cats.effect.IO
import ironoxide.v1.user.{Jwt => IronCoreJwt}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import scala.io.Source

import java.time.Instant

object JwtSigner {

  private val _ = System.loadLibrary("ironoxide_java")

  private val PRIVATE_KEY: String =
    Source
      .fromFile(sys.env("JWT_SIGNING_KEY"))
      .mkString

  /** Constructs default JWT claims for this project. */
  def ironcoreClaims(user: String, expires: Long): JwtClaim = JwtClaim(
    content = s"""{
        "pid": 3323,
        "sid": "ZGJpby10ZXN0aW5n",
        "kid": 4162,
        "sub": "$user",
        "exp": $expires
      }""",
    issuedAt = Some(Instant.now().getEpochSecond())
  )

  /** Generates IronCore's internal JWT for a given user. */
  def forUser(user: String): IO[IronCoreJwt] = {
    val encoded = JwtCirce.encode(
      ironcoreClaims(user, 3600),
      PRIVATE_KEY,
      JwtAlgorithm.ES256
    )
    IronCoreJwt.validate[IO](encoded)
  }

}

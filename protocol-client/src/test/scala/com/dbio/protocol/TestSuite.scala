package com.dbio.protocol

import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.syntax._
import ironoxide.v1.common.UserId
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

class TestSuite extends AsyncFunSuite with AsyncIOSpec with Matchers {
  test("Round trip encrypt/decrypt with IronCore") {
    val plaintext = """{"id":"myResourceId","type":"Patient"}""".asJson
    val prog = for {
      user1 <- IronCore.forUser("testUser1", "password")
      user2 <- IronCore.forUser("testUser2", "password")
      enc <- IronCore
        .transferEncrypt(plaintext, UserId("testUser1"), UserId("testUser2"))
        .run(user1)
      out1 <- IronCore.decrypt(enc.encryptedData.toBase64).run(user1)
      out2 <- IronCore.decrypt(enc.encryptedData.toBase64).run(user2)
    } yield out1 -> out2
    prog.asserting { case (j1, j2) =>
      j1 should ===(j2)
      j1 should ===(plaintext)
    }
  }
}

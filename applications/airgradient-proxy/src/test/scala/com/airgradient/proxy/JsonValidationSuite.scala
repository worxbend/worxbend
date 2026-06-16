package com.airgradient.proxy

import com.airgradient.proxy.upstream.JsonValidator
import com.airgradient.proxy.upstream.UpstreamError
import munit.FunSuite

class JsonValidationSuite extends FunSuite:

  test("valid AirGradient JSON passes validation") {
    val json = """{"wifi":{"rssi":-50},"pm01":1.2,"pm02":2.0}"""
    assert(JsonValidator.validate(json.getBytes("UTF-8")).isRight)
  }

  test("empty JSON object passes validation") {
    assert(JsonValidator.validate("{}".getBytes("UTF-8")).isRight)
  }

  test("JSON array passes validation") {
    assert(JsonValidator.validate("[1,2,3]".getBytes("UTF-8")).isRight)
  }

  test("malformed JSON fails validation") {
    val result = JsonValidator.validate("{not valid json".getBytes("UTF-8"))
    assert(result.isLeft)
    result match
      case Left(e: UpstreamError.InvalidJson) => assert(e.message.nonEmpty)
      case other                              => fail(s"Expected InvalidJson, got $other")
  }

  test("empty bytes fail validation") {
    val result = JsonValidator.validate(Array.emptyByteArray)
    assert(result.isLeft)
  }

  test("unknown JSON fields are preserved in raw bytes") {
    val json  = """{"unknown_field_firmware_v99":"some_value","pm01":1.2}"""
    val bytes = json.getBytes("UTF-8")
    assert(JsonValidator.validate(bytes).isRight)
    assertEquals(new String(bytes, "UTF-8"), json)
  }

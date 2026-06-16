package com.airgradient.proxy.upstream

import com.github.plokhotnyuk.jsoniter_scala.core.*

import scala.util.control.NonFatal

object JsonValidator:

  // Scans and discards any JSON value — validates structure without deserializing.
  private val scanCodec: JsonValueCodec[Unit] = new JsonValueCodec[Unit]:
    def decodeValue(in: JsonReader, default: Unit): Unit = in.skip()
    def encodeValue(x: Unit, out: JsonWriter): Unit      = out.writeNull()
    def nullValue: Unit                                   = ()

  def validate(bytes: Array[Byte]): Either[UpstreamError, Unit] =
    try
      readFromArray[Unit](bytes)(using scanCodec)
      Right(())
    catch
      case e: JsonReaderException => Left(UpstreamError.InvalidJson(s"invalid JSON from upstream: ${e.getMessage}"))
      case NonFatal(e)            => Left(UpstreamError.InvalidJson(s"JSON validation failed: ${e.getMessage}"))

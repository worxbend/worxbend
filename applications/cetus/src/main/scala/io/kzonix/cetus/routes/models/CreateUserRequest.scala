package io.kzonix.cetus.routes.models

import zio.json.DeriveJsonEncoder
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames

@jsonMemberNames(SnakeCase)
case class CreateUserRequest(
    username: String,
    password: String,
    email:    String,
  ):
  override def toString: String = s"CreateUserRequest(username=$username, email=$email, password=***)"
object CreateUserRequest:
  implicit val encoder: JsonEncoder[CreateUserRequest] = DeriveJsonEncoder.gen[CreateUserRequest]

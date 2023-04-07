package io.kzonix.cetus.routes.models

import zio.json.DeriveJsonEncoder
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames

@jsonMemberNames(SnakeCase)
case class CreateUserModel(
    username: String,
    password: String,
    email:    String,
  ):
  override def toString: String = s"CreateUserModel(username=$username, email=$email, password=***)"
object CreateUserModel:
  implicit val encoder: JsonEncoder[CreateUserModel] = DeriveJsonEncoder.gen[CreateUserModel]

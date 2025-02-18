package io.worxbend.describo

import io.worxbend.describo.Main.Foo.given
import io.worxbend.describo.ToString.Configuration
import io.worxbend.describo.ToString.annotations.Redacted

object Main:

  def main(args: Array[String]): Unit =
    //
    println(
      Foo(
        "test",
        "test",
        1,
        1,
        1,
        1,
        1.0,
        1.0f,
      )
    )

  case class Foo(
      @transient name:                                String,
      name2:                                          String,
      age:                                            Int,
      age2:                                           Int,
      age3:                                           Int,
      age4:                                           Int,
      height:                                         Double,
      weight:                                         Float,
      @Redacted(replacement = "<redacted>") password: String = "<PASSWORD>",
  ) {
    override def toString: String = ToString.derived(this)
  }

  object Foo:

    given configuration: ToString.Configuration =
      ToString.Configuration(
        multilineIfFieldsAreGreaterOrEqual = -1
      )

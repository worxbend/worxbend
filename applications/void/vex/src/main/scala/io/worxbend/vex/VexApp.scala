package io.worxbend.vex


object VexApp {

  val name = "VexApp"

  def main(args: Array[String]): Unit = {
    val name = "main::" + VexApp.name
    println(s"Hello, world! $name")
    println(s"Hello, world! ${Helpers.print()}")
  }
}


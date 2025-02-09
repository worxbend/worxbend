package com.worxbend.sandbox

import com.worxbend.sandbox.CaseClassMacros.Print
import com.worxbend.sandbox.Macros2.Configuration

import java.time.LocalTime

object SandboxApp extends App:

  case class Person(name: String, age: Int) {
    override val toString: String = "it is me"
  }

  private val maria: Person = Person.apply("Maria", 25)
  println(maria)
  println(maria)
  println(maria)

  println(Macros.inspect("x"))
  println(Macros.test(ignore = true, computation = println("eval")))

  def test(): Unit = {
    def printSymbols = Macros.printSymbols()
    printSymbols()
  }

  test()

  println(Macros.natConst(4))

  sealed trait Person2
  case class Alive(time: LocalTime, child: Person2) extends Person2 derives CaseClassMacros.Print {
    override val toString: String = this.print
  }

  case class Dead(time: LocalTime) extends Person2 derives CaseClassMacros.Print

  println(Alive(LocalTime.now(), child = Alive(LocalTime.now(), child = Dead(LocalTime.now()))))
  val seq = Seq(
    Alive(LocalTime.now(), child = Alive(LocalTime.now(), child = Dead(LocalTime.now()))),
    Alive(LocalTime.now(), child = Alive(LocalTime.now(), child = Dead(LocalTime.now()))),
    Alive(LocalTime.now(), child = Alive(LocalTime.now(), child = Dead(LocalTime.now()))),
  )

  println(s"List: $seq")

  case class A(
      a:    Int,
      b:    Boolean = false,
      name: String = "default",
    ) {

    import A.configuration
    override val toString: String = Macros2.toString(this)

  }
  object A {
    given configuration: Configuration = Configuration()
  }

  println(A(a = 1))
  println(A(a = 1))
  println(A(a = 1))
  println(A(a = 1))

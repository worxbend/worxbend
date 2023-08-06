package com.worxbend.sandbox

import com.worxbend.sandbox.Macros2.Configuration

import java.time.LocalTime

object SandboxApp extends App:

  case class Person(name: String, age: Int) {
    override val toString: String = {
      println("Calculating")
      "it is me"
    }
  }

  private val maria: Person = Person.apply("Maria", 25)
  println(maria)
  println(maria)
  println(maria)

  println(Macros.inspect("x"))
  println(Macros.test(ignore = true, computation = println("eval")))

  def test() = {
    def printSymbols = Macros.printSymbols()

    // It will print: Splice owner: macro, parent printSymbols, grandParent symbolTest
    printSymbols()
  }

  test()

  println(Macros.natConst(4))

  sealed trait Person2 derives CaseClassMacros.Print
  case class Alive(time: LocalTime, child: Person2) extends Person2

  case class Dead(time: LocalTime) extends Person2

  println(Alive(LocalTime.now(), child = Alive(LocalTime.now(), child = Dead(LocalTime.now()))).print)

  case class A(a: Int, name: String = "default") {

    private val strRepr           = Macros2.toString(this)
    override def toString: String = strRepr

  }
  object A {
    given configuration: Configuration = Configuration()
  }

  println(A(a = 1))
  println(A(a = 1))
  println(A(a = 1))
  println(A(a = 1))

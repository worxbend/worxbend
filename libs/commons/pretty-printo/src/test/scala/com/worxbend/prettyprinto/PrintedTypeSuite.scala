package com.worxbend.prettyprinto

import org.scalatest.funsuite.AnyFunSuite

class PrintedTypeSuite extends AnyFunSuite:

  private val listType: PrintedType = PrintedType("List", "scala.collection.immutable.List")

  test("compressPackage shortens every leading lowercase segment to one character"):
    assert(PrintedType.compressPackage("com.worxbend.prettyprinto.Fixture") == "c.w.p.Fixture")

  test("compressPackage leaves a name without a package untouched and adds no leading dot"):
    assert(PrintedType.compressPackage("Fixture") == "Fixture")

  test("compressPackage keeps every segment after the first capitalised one"):
    assert(PrintedType.compressPackage("com.worxbend.prettyprinto.Fixtures.Inner") == "c.w.p.Fixtures.Inner")

  test("compressPackage handles a single-segment package"):
    assert(PrintedType.compressPackage("scala.Int") == "s.Int")

  test("render returns the simple name when fully qualified names are off"):
    assert(listType.render(using Configuration.default) == "List")

  test("render returns the qualified name when the short package prefix is off"):
    val conf = Configuration(fullyQualifiedClassName = true, shortPackagePrefix = false)
    assert(listType.render(using conf) == "scala.collection.immutable.List")

  test("render returns the compressed name when the short package prefix is on"):
    val conf = Configuration(fullyQualifiedClassName = true, shortPackagePrefix = true)
    assert(listType.render(using conf) == "s.c.i.List")

  test("shortPackagePrefix has no effect while fullyQualifiedClassName is off"):
    assert(listType.render(using Configuration(shortPackagePrefix = false)) == "List")

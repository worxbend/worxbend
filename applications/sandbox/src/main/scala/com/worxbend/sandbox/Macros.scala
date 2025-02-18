package com.worxbend.sandbox

import scala.quoted.*

object Macros {

  def inspectCode(
      x: Expr[Any]
  )(using Quotes): Expr[Any] =
    println("inspection of " + x.show)
    x

  inline def inspect(inline x: Any): Any                                 = ${ inspectCode('x) }
  inline def test(inline ignore: Boolean, computation: => Unit): Boolean = ${ testCode('ignore, 'computation) }

  def testCode(
      ignore: Expr[Boolean],
      computation: Expr[Unit],
  )(using quotes: Quotes) =
    if ignore.valueOrAbort then Expr(false)
    else Expr.block(List(computation), Expr(true))

  def printSymbolsImpl(
      using q: Quotes
  ): Expr[Function0[Unit]] = {
    import quotes.reflect._

    val owner       = Expr(Symbol.spliceOwner.name)
    val parent      = Expr(Symbol.spliceOwner.owner.name)
    val grandParent = Expr(Symbol.spliceOwner.owner.owner.name)

    // The body of the function can be created with quoting and splicing.
    // It can be also created with use of reflection module.
    val functionBody: Expr[Unit] =
      '{
        println(s"Splice owner: ${$owner}, parent ${$parent}, grandParent ${$grandParent}")
      }

    // To create a new method Symbol function is used.
    val functionDefSymbol = Symbol.newMethod(
      Symbol.spliceOwner,      // Symbol - owner of the method
      "printSymbolsGenerated", // The name of the function
      MethodType(Nil)(         // Argument names - in this case empty list
        _ => Nil,              // Argument definitions - in this case empty list
        _ => TypeRepr.of[Unit], // Return type
      ),
    )

    // The definition of the function.
    val functionDef = DefDef(functionDefSymbol, { case _ => Some(functionBody.asTerm.changeOwner(functionDefSymbol)) })

    // The DefDef will not evaluate to Expr (call to isExpr of this term returns false) so it needs to be
    // included into the AST below. It was found with use of logAST on an empty function.
    Block(List(functionDef), Closure(Ref(functionDefSymbol), None)).asExprOf[() => Unit]
  }

  inline def printSymbols(): () => Unit = ${ printSymbolsImpl }

  inline def natConst(inline x: Int): Int = ${ natConstImpl('{ x }) }

  def natConstImpl[T <: Product](x: Expr[Int])(using Quotes): Expr[Int] =
    import quotes.reflect.*
    val tree: Term = x.asTerm
    tree match
      case Inlined(_, _, Literal(IntConstant(n))) =>
        if n <= 0 then
          report.error("Parameter must be natural number")
          '{ 0 }
        else
          tree.asExprOf[Int]
      case _                                      =>
        report.error("Parameter must be a known constant")
        '{ 0 }

}

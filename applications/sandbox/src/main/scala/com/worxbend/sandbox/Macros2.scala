package com.worxbend.sandbox

import scala.quoted.*

object Macros2 {

  def toStringImpl[T <: Product](
      x: Expr[T],
      cfg: Expr[Configuration],
    )(using
      Type[T]
    )(using
      quotes: Quotes
    ): Expr[String] = {
    import quotes.reflect.*

    val printValue: Expr[Any => String] =
      '{ (v: Any) =>
        v match {
          case s: String => s"\"$s\""
          case any       => any.toString
        }
      }

    lazy val string: Expr[String] =
      '{
        val f: Any => String = $printValue

        val configuration = $cfg

        val input: T = $x

        val product              = input
        val productLabeledValues = product
          .productElementNames
          .zipWithIndex
          .map {
            case (name, i) =>
              val valueStr = f(input.productElement(i))
              val labelStr = s"${ configuration.labelWrappingCharacter }$name${ configuration.labelWrappingCharacter }"
              s"$labelStr${ configuration.labelCharacter } $valueStr"
          }
          .mkString(s"${ configuration.elementsSeparator.toString } ")
        val (l, r)               = configuration.wrappingCharacters
        s"${ product.productPrefix }$l$productLabeledValues$r"
      }
    string

  }

  case class Configuration(
      labelCharacter:         Char = ':',
      labelWrappingCharacter: Char = '\"',
      wrappingCharacters:     (Char, Char) = ('(', ')'),
      elementsSeparator:      Char = ',',
    )

  inline def toString[T <: Product](
      inline x: T
    )(using
      inline configuration: Configuration
    ): String = ${ toStringImpl[T]('x, 'configuration) }

}

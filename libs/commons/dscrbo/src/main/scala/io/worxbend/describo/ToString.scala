package io.worxbend.describo

import io.worxbend.describo.ToString.annotations.Excluded
import io.worxbend.describo.ToString.annotations.Redacted

import scala.annotation.StaticAnnotation
import scala.quoted.*

object ToString {

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

    // Extract case class type
    val tpe: TypeRepr = TypeRepr.of[T]

    // Ensure it's a case class
    if (!tpe.typeSymbol.flags.is(Flags.Case))
      report.error(s"${ tpe.show } is not a case class!")

    val excludedAnnotation  = TypeRepr.of[Excluded].typeSymbol
    val redactedAnnotation  = TypeRepr.of[Redacted].typeSymbol
    val transientAnnotation = TypeRepr.of[transient].typeSymbol

    // Extract field names and annotations
    val intructions: Seq[Expr[Option[(String, Boolean, String)]]] = tpe
      .typeSymbol
      .caseFields
      .collect:
        case field if field.hasAnnotation(redactedAnnotation)                                             =>
          val fieldNameExpr = Expr(field.name.asInstanceOf[String])
          val redactedExpr  = field.getAnnotation(redactedAnnotation).get.asExprOf[Redacted]
          val keep          = Expr.apply(true)
          '{
            Some(
              $fieldNameExpr,
              $keep,
              $redactedExpr.replacement,
            )
          }
        case field if field.hasAnnotation(excludedAnnotation) || field.hasAnnotation(transientAnnotation) =>
          val fieldNameExpr = Expr(field.name.asInstanceOf[String])
          val replacement   = Expr("")
          val keep          = Expr.apply(false)
          '{
            Some(
              $fieldNameExpr,
              $keep,
              $replacement,
            )
          }

    val intructionsExpr: Expr[Seq[Option[(String, Boolean, String)]]] = Expr.ofSeq(intructions)
    val skipOrReplaceInstructExpr                                     = '{ $intructionsExpr.toVector }

    lazy val toStringExpr: Expr[String] =
      '{
        val f: Any => String = $printValue

        val conf = $cfg

        val input: T = $x

        val skipOrReplaceInstructionsMap: Map[String, (Boolean, String)] =
          ($skipOrReplaceInstructExpr)
            .view
            .flatten
            .map { case (fieldName, shouldKeep, replacementValue) => fieldName -> (shouldKeep, replacementValue) }
            .toMap

        val product = input

        val productElementNames =
          product
            .productElementNames
            .zipWithIndex

        val fieldsSep = if conf.multiline then conf.fieldsSeparator.toString + "\n" else conf.fieldsSeparator.toString

        val fieldsAndValues = productElementNames
          .toList
          .flatMap {
            case (name, i) =>
              val productElement                                  = input.productElement(i)
              val (keep: Boolean, replacementVal: Option[String]) =
                skipOrReplaceInstructionsMap.get(name) match {
                  case Some(keep, replacementVal) => (keep, Some(replacementVal))
                  case None                       => (true, None)
                }

              val fieldValue =
                replacementVal match {
                  case Some(replacementVal) => s"${ conf.valuePrefix }$replacementVal${ conf.valueSuffix }"
                  case None                 => s"${ conf.valuePrefix }${ f(productElement) }${ conf.valueSuffix }"
                }
              val fieldType  =
                if conf.useTypeNames then
                  s"${ conf.fieldNameAndTypeNameSeparator }" +
                    s"${ conf.typeNamePrefix }${ productElement.getClass.getSimpleName }${ conf.typeNameSuffix }"
                else ""
              val fieldName  =
                if conf.useFieldNames then
                  s"${ conf.fieldNamePrefix }$name${ conf.fieldNameSuffix }$fieldType${ conf.fieldNameAndValueSeparator }"
                else ""

              if keep then Some(fieldName + fieldValue)
              else None

          }

        val (sep, lf, indent)        =
          if conf.multiline || (conf.multilineIfFieldsAreGreaterOrEqual > 0 && fieldsAndValues.size >= conf.multilineIfFieldsAreGreaterOrEqual)
          then (fieldsSep.trim + "\n", "\n", conf.multilineIndent)
          else (fieldsSep, "", "")
        val formattedFieldsAndValues = fieldsAndValues.map(indent + _).mkString(s"$sep")
        s"${ product.productPrefix }($lf" +
          s"$formattedFieldsAndValues$lf" +
          s")"
      }

    '{ $toStringExpr }
  }

  object annotations:

    import scala.annotation.StaticAnnotation
    import scala.annotation.meta.*

    @field
    final class Excluded extends StaticAnnotation

    @field
    final class Redacted(val replacement: String = "<redacted>") extends StaticAnnotation

  case class Configuration(
      useFieldNames:                      Boolean = true,
      useTypeNames:                       Boolean = false,
      fieldsSeparator:                    String = ", ",
      fieldNamePrefix:                    String = "",
      fieldNameSuffix:                    String = "",
      fieldNameAndValueSeparator:         String = " = ",
      fieldNameAndTypeNameSeparator:      String = ": ",
      typeNamePrefix:                     String = "",
      typeNameSuffix:                     String = "",
      valuePrefix:                        String = "",
      valueSuffix:                        String = "",
      multiline:                          Boolean = false,
      multilineIndent:                    String = "  ",
      multilineIfFieldsAreGreaterOrEqual: Int = 5,
    )

  inline def derived[T <: Product](
      inline x: T
    )(using
      inline configuration: Configuration = Configuration()
    ): String = ${ toStringImpl[T]('x, 'configuration) }

}

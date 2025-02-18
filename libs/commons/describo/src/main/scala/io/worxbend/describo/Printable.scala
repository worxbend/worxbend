package io.worxbend.describo

import io.worxbend.describo.Printable.Configuration
import io.worxbend.describo.Printable.annotations.Excluded
import io.worxbend.describo.Printable.annotations.Redacted

import scala.jdk.CollectionConverters.*

import java.time.LocalDate
import java.time.LocalTime

import magnolia1.*

// Prints a type, only requires read access to fields
trait Printable[T]:

  extension (x: T)

    def asString(
        using configuration: Configuration = Configuration()
    ): String

trait GenericPrint extends AutoDerivation[Printable]:

  def join[T](ctx: CaseClass[Typeclass, T]): Printable[T] =
    new Printable[T]:
      extension (value: T)
        override def asString(
            using configuration: Configuration
        ): String =
          if ctx.isValueClass then
            val param = ctx.params.head
            param.typeclass.asString(param.deref(value))
          else

            val filteredParams = ctx
              .params
              .view
              .filter { param =>
                val maybeExcluded = param.annotations.toList.collectFirst {
                  case el: Excluded  => el
                  case el: transient => el
                }
                maybeExcluded.isEmpty
              }
            val size           = filteredParams.size

            val (del: String, lf: String, indent: String) = multiline(size)
            filteredParams
              .map { param =>
                indent +
                  (if configuration.useFieldNames then
                     fieldName(param) +
                       fieldType(value, param) +
                       sep
                   else "") +
                  fieldValue(value, param)
              }
              .mkString(
                s"${typeName(ctx)}($lf",
                s"$del$lf",
                s"$lf)",
              )

  private def multiline[T](
      size: Int
  )(using conf: Configuration) = {
    val (del, multilineChar, indent) =
      if conf.multiline || (conf.multilineIfFieldsAreGreaterOrEqual > 0 && size >= conf.multilineIfFieldsAreGreaterOrEqual)
      then (",", "\n", conf.multilineIndent)
      else (", ", "", "")
    (del, multilineChar, indent)
  }

  private def typeName[T](
      ctx: CaseClass[Typeclass, T]
  )(using conf: Configuration) =
    if conf.fullyQualifiedClassName then
      val fullName = ctx.typeInfo.full
      if conf.shortPackagePrefix then
        val parts     = fullName.split('.')
        val untouched = parts.last
        val shortened = parts.tail.map(_.head).mkString(".")
        shortened + "." + untouched
      else fullName
    else ctx.typeInfo.short

  private def fieldValue[T](
      value: T,
      param: CaseClass.Param[Typeclass, T],
  )(using conf: Configuration) =
    conf.valuePrefix + {
      val maybeSensitiveData = param.annotations.toList.collectFirst { case el: Redacted => el }

      maybeSensitiveData match
        case Some(sensitiveData: Redacted) => sensitiveData.replacement
        case None                          => param.typeclass.asString(param.deref(value))

    }
      + conf.valueSuffix

  private def fieldType[T](
      value: T,
      param: CaseClass.Param[Printable, T],
  )(using conf: Configuration) =
    if (conf.useTypeNames)
      conf.fieldNameAndTypeNameSeparator +
        conf.typeNamePrefix + (if conf.fullyQualifiedClassName then param.deref(value).getClass.getName
                               else param.deref(value).getClass.getSimpleName) + conf.typeNameSuffix
    else
      ""

  private def sep(
      using conf: Configuration
  ) = conf.fieldNameAndValueSeparator

  private def fieldName[T](
      param: CaseClass.Param[Printable, T]
  )(using conf: Configuration) = conf.fieldNamePrefix + param.label + conf.fieldNameSuffix

  override def split[T](ctx: SealedTrait[Typeclass, T]): Printable[T] =
    new Printable[T]:
      extension (value: T)
        override def asString(
            using configuration: Configuration
        ): String = ctx.choose(value)(sub => sub.typeclass.asString(sub.value))

trait AutoToString:

  given p: Printable[this.type] = scala.compiletime.deferred
  given c: Configuration        = scala.compiletime.deferred
  override def toString: String = summon[Printable[this.type]].asString(this)

object Printable extends GenericPrint:

  object annotations:

    import scala.annotation.meta._

    @field
    final class Excluded extends scala.annotation.StaticAnnotation

    @field
    final class Redacted(val replacement: String = "<redacted>") extends scala.annotation.StaticAnnotation

  case class Configuration(
      useFieldNames:                      Boolean = true,
      useTypeNames:                       Boolean = false,
      fullyQualifiedClassName:            Boolean = false,
      shortPackagePrefix:                 Boolean = true,
      fieldsSeparator:                    String = ",",
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

  object Configuration:
    val default: Configuration = Configuration()
  end Configuration

  given Printable[String] with

    extension (x: String)

      override def asString(
          using configuration: Configuration
      ): String = s"\"$x\""

  given Printable[Int] with

    extension (x: Int)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Double] with

    extension (x: Double)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Boolean] with

    extension (x: Boolean)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[BigInt] with

    extension (x: BigInt)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[BigDecimal] with

    extension (x: BigDecimal)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Short] with

    extension (x: Short)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Char] with

    extension (x: Char)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Byte] with

    extension (x: Byte)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Long] with

    extension (x: Long)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[Float] with

    extension (x: Float)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.lang.Integer] with

    extension (x: java.lang.Integer)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.lang.Character] with

    extension (x: java.lang.Character)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[LocalDate] with

    extension (x: LocalDate)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[LocalTime] with

    extension (x: LocalTime)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.Instant] with

    extension (x: java.time.Instant)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.Duration] with

    extension (x: java.time.Duration)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.Period] with

    extension (x: java.time.Period)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.ZonedDateTime] with

    extension (x: java.time.ZonedDateTime)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.OffsetDateTime] with

    extension (x: java.time.OffsetDateTime)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given Printable[java.time.OffsetTime] with

    extension (x: java.time.OffsetTime)

      override def asString(
          using configuration: Configuration
      ): String = x.toString

  given map[K, V](
      using keyP: Printable[K],
      valueP: Printable[V],
  ): Printable[Map[K, V]] with

    extension (x: Map[K, V])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map { case (key, value) => s"${keyP.asString(key)} -> ${valueP.asString(value)}" }
          .mkString(
            "[",
            ", ",
            "]",
          )

  given seq[T](
      using printableT: Printable[T]
  ): Printable[Iterable[T]] with

    extension (x: Iterable[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given list[T](
      using printableT: Printable[T]
  ): Printable[List[T]] with

    extension (x: List[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given set[T](
      using printableT: Printable[T]
  ): Printable[Set[T]] with

    extension (x: Set[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given vector[T](
      using printableT: Printable[T]
  ): Printable[Vector[T]] with

    extension (x: Vector[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given array[T](
      using printableT: Printable[T]
  ): Printable[Array[T]] with

    extension (x: Array[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given javaList[T](
      using printableT: Printable[T]
  ): Printable[java.util.List[T]] with

    extension (x: java.util.List[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given javaSet[T](
      using printableT: Printable[T]
  ): Printable[java.util.Set[T]] with

    extension (x: java.util.Set[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given javaMap[K, V](
      using keyP: Printable[K],
      valueP: Printable[V],
  ): Printable[java.util.Map[K, V]] with

    extension (x: java.util.Map[K, V])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map { case (key, value) => s"${keyP.asString(key)} -> ${valueP.asString(value)}" }
          .mkString(
            "[",
            ", ",
            "]",
          )

  given javaHashMap[K, V](
      using keyP: Printable[K],
      valueP: Printable[V],
  ): Printable[java.util.HashMap[K, V]] with

    extension (x: java.util.HashMap[K, V])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map { case (key, value) => s"${keyP.asString(key)} -> ${valueP.asString(value)}" }
          .mkString(
            "{",
            ", ",
            "}",
          )

  given javaLinkedList[T](
      using printableT: Printable[T]
  ): Printable[java.util.LinkedList[T]] with

    extension (x: java.util.LinkedList[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given javaArrayList[T](
      using printableT: Printable[T]
  ): Printable[java.util.ArrayList[T]] with

    extension (x: java.util.ArrayList[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x.asScala
          .map(printableT.asString)
          .mkString(
            "[",
            ", ",
            "]",
          )

  given option[T](
      using printableT: Printable[T]
  ): Printable[Option[T]] with

    extension (x: Option[T])

      override def asString(
          using configuration: Configuration
      ): String =
        x match
          case Some(value) => s"Some(${printableT.asString(value)})"
          case None        => "None"

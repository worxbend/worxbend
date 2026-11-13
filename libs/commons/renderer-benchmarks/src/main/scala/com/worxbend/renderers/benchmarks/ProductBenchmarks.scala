package com.worxbend.renderers.benchmarks

import com.worxbend.prettyprinto.{ Configuration as PrintoConfiguration, PrettyPrintable as Printo }
import com.worxbend.prettyprinto.annotations.Redacted as PrintoRedacted
import com.worxbend.reveal.{ Configuration as RevealConfiguration, PrettyPrintable as Reveal }
import com.worxbend.reveal.annotations.Redacted as RevealRedacted

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

object ProductModels:

  final case class RevealProduct(id: Int, text: String, active: Boolean) derives Reveal
  final case class PrintoProduct(id: Int, text: String, active: Boolean) derives Printo
  final case class RevealSecret(id: Int, @RevealRedacted token: String) derives Reveal
  final case class PrintoSecret(id: Int, @PrintoRedacted token: String) derives Printo
  final case class RevealNested(child: RevealProduct, optional: Option[RevealSecret]) derives Reveal
  final case class PrintoNested(child: PrintoProduct, optional: Option[PrintoSecret]) derives Printo

/** Returned strings keep rendering observable to JMH. Fixtures stay outside the timed methods. */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = Array("-Xms512m", "-Xmx512m"))
class ProductBenchmarks:

  import ProductModels.*

  private given RevealConfiguration = RevealConfiguration(multilineIfFieldsAreGreaterOrEqual = -1)
  private given PrintoConfiguration = PrintoConfiguration(multilineIfFieldsAreGreaterOrEqual = -1)

  private val revealProduct = RevealProduct(42, "line\n\"quoted\"\\path", true)
  private val printoProduct = PrintoProduct(42, "line\n\"quoted\"\\path", true)
  private val revealSecret  = RevealSecret(42, "benchmark-fixture")
  private val printoSecret  = PrintoSecret(42, "benchmark-fixture")
  private val revealNested  = RevealNested(revealProduct, Some(revealSecret))
  private val printoNested  = PrintoNested(printoProduct, Some(printoSecret))
  private val revealPrinter = summon[Reveal[RevealProduct]]
  private val printoPrinter = summon[Printo[PrintoProduct]]

  @Benchmark def revealFlat(): String      = revealProduct.asString
  @Benchmark def printoFlat(): String      = printoProduct.asString
  @Benchmark def revealRedacted(): String  = revealSecret.asString
  @Benchmark def printoRedacted(): String  = printoSecret.asString
  @Benchmark def revealDelegated(): String = revealNested.asString
  @Benchmark def printoDelegated(): String = printoNested.asString
  @Benchmark def revealNull(): String      = revealPrinter.describe(null)
  @Benchmark def printoNull(): String      = printoPrinter.asString(null)

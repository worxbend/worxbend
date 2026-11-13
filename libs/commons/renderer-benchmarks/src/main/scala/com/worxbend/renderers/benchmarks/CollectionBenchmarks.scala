package com.worxbend.renderers.benchmarks

import com.worxbend.prettyprinto.{ Configuration as PrintoConfiguration, PrettyPrintable as Printo }
import com.worxbend.reveal.{ Configuration as RevealConfiguration, PrettyPrintable as Reveal }

import scala.compiletime.uninitialized

import scala.collection.immutable.ListMap

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

object CollectionModels:

  final case class RevealBatch(entries: List[String], lookup: Map[String, Int]) derives Reveal
  final case class PrintoBatch(entries: List[String], lookup: Map[String, Int]) derives Printo

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = Array("-Xms512m", "-Xmx512m"))
class CollectionBenchmarks:

  import CollectionModels.*

  @Param(Array("0", "1", "32", "1024"))
  var size: Int = 0

  @Param(Array("false", "true"))
  var multiline: Boolean = false

  // JMH injects parameters before trial setup; each worker owns its state.
  private var revealBatch: RevealBatch                 = uninitialized
  private var printoBatch: PrintoBatch                 = uninitialized
  private var revealConfiguration: RevealConfiguration = uninitialized
  private var printoConfiguration: PrintoConfiguration = uninitialized

  @Setup(Level.Trial)
  def setup(): Unit =
    val entries = List.tabulate(size)(index => s"entry-$index\n")
    val lookup  = ListMap.from(entries.zipWithIndex)
    revealBatch = RevealBatch(entries, lookup)
    printoBatch = PrintoBatch(entries, lookup)
    revealConfiguration = RevealConfiguration(multiline = multiline, multilineIfFieldsAreGreaterOrEqual = -1)
    printoConfiguration = PrintoConfiguration(multiline = multiline, multilineIfFieldsAreGreaterOrEqual = -1)

  @Benchmark def revealCollections(): String = revealBatch.asString(using revealConfiguration)
  @Benchmark def printoCollections(): String = printoBatch.asString(using printoConfiguration)

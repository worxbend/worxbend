package consumer

import com.worxbend.reveal.*
import com.worxbend.reveal.annotations.Redacted

object Consumer:

  case object Singleton
  final case class Secret(@Redacted token: String) derives PrettyPrintable
  final case class Box(values: List[Option[Secret]]) derives PrettyPrintable
  sealed trait Event

  case object Stop extends Event:

    given PrettyPrintable[Stop.type] with
      override def describe(value: Stop.type)(using Configuration): String = "CUSTOM"

  final case class Envelope(event: Event) derives PrettyPrintable

  def main(args: Array[String]): Unit =
    var evaluations              = 0
    def next(): Singleton.type   =
      evaluations += 1
      Singleton
    val rendered                 = ToString.derived(next())
    assert(rendered == "Singleton")
    assert(evaluations == 1)
    val failure                  = IllegalStateException("producer failed")
    def broken(): Singleton.type = throw failure
    val caught                   =
      try
        val unexpected = ToString.derived(broken())
        throw AssertionError(s"Producer should have thrown, rendered $unexpected")
      catch case thrown: IllegalStateException => thrown
    assert(caught eq failure)
    assert(Box(List(Some(Secret("marker")))).asString == "Box(values = [Some(Secret(token = <redacted>))])")
    assert(Envelope(Stop).asString == "Envelope(event = CUSTOM)")
    println("External consumer: passed")

package io.worxbend.vex


sealed trait Entry {
  def name: String
}
object Entry {

    case class StringEntry(name: String, value: String) extends Entry
    case class IntEntry(name: String, value: Int) extends Entry
    case class DoubleEntry(name: String, value: Double) extends Entry
    case class BooleanEntry(name: String, value: Boolean) extends Entry
    case class ListEntry(name: String, value: List[Entry]) extends Entry
    case class MapEntry(name: String, value: Map[String, Entry]) extends Entry

    def print(entry: Entry): String = {
      entry match {
        case StringEntry(name, value) => s"$name: $value"
        case IntEntry(name, value) => s"$name: $value"
        case DoubleEntry(name, value) => s"$name: $value"
        case BooleanEntry(name, value) => s"$name: $value"
        case ListEntry(name, value) => s"$name: ${value.map(print).mkString(", ")}"
        case MapEntry(name, value) => s"$name: ${value.map { case (k, v) => s"$k: ${print(v)}" }.mkString(", ")}"
      }
    }
  
}

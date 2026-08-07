package com.worxbend.describo

/** Value-level rendering shared by the derived instances and the built-in givens. */
private object Rendering:

  /** The four characters printed for a `null` value, in every position. */
  val nullLiteral: String = "null"

  /** Separator between collection elements and between map entries. Deliberately not `fieldsSeparator`, which is an
    * inter-field knob only.
    */
  val elementSeparator: String = ", "

  /** Escapes backslash, double quote, newline, carriage return and tab, in that order. Nothing else is escaped, so the
    * two modules stay trivially identical.
    */
  def escaped(raw: String): String =
    raw
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

  /** Renders a value through its typeclass, short-circuiting `null` to the bare `null` literal. The explicit branch is
    * what makes the null / redaction / omission ordering auditable.
    */
  def value[A](element: A)(using printable: Printable[A])(using Configuration): String =
    if element == null then nullLiteral else printable.asString(element)

  /** `[a, b, c]` over an already-null-checked container. */
  def elements[A](iterator: Iterator[A])(using Printable[A])(using Configuration): String =
    iterator.map(element => value(element)).mkString("[", elementSeparator, "]")

  /** `[k -> v]` over an already-null-checked container. */
  def entries[K, V](iterator: Iterator[(K, V)])(using Printable[K], Printable[V])(using Configuration): String =
    iterator
      .map((key, entryValue) => s"${value(key)} -> ${value(entryValue)}")
      .mkString("[", elementSeparator, "]")

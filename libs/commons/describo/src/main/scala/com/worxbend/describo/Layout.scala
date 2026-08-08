package com.worxbend.describo

/** How a product's rendered fields are laid out. Chosen once per render. */
private enum Layout:

  case SingleLine
  case Multiline

private object Layout:

  /** Multiline is chosen when it is asked for outright or when enough fields survived exclusion.
    *
    * `multilineIfFieldsAreGreaterOrEqual <= 0` disables the threshold; that sentinel is laundered into a [[Layout]]
    * here rather than being reinterpreted at every use site. Zero rendered fields always collapse to a single line so
    * that an empty or fully excluded type renders `T()` even under `multiline = true`.
    */
  def of(renderedFieldCount: Int)(using conf: Configuration): Layout =
    if renderedFieldCount == 0 then Layout.SingleLine
    else if conf.multiline then Layout.Multiline
    else if conf.multilineIfFieldsAreGreaterOrEqual > 0 && renderedFieldCount >= conf.multilineIfFieldsAreGreaterOrEqual
    then Layout.Multiline
    else Layout.SingleLine

package consumer

import com.worxbend.reveal.PrettyPrintable
import com.worxbend.reveal.annotations.Redacted

sealed trait Values extends java.lang.Iterable[String]

final case class SecretValues(@Redacted token: String) extends Values:
  override def iterator(): java.util.Iterator[String] = java.util.Collections.emptyIterator[String]()

final case class Middle(payload: Values)

final case class Outer(middle: Middle) derives PrettyPrintable

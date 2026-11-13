package consumer

import com.worxbend.reveal.PrettyPrintable

final case class Growth[A](next: Option[Growth[List[A]]], tag: String)

final case class GrowingOuter(payload: Growth[Int]) derives PrettyPrintable

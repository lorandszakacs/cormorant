package io.chrisdavenport.cormorant.fs2

import cats.data.NonEmptyList
import cats.effect.IO
import fs2.Stream
import io.chrisdavenport.cormorant._
import io.chrisdavenport.cormorant.implicits._

class StreamingParseSpec extends CormorantSpec {

  "Streaming printer should" in {

    "row should round trip" in prop { a: CSV.Row =>
      Stream
        .emit[IO, CSV.Row](a)
        .through(encodeRows(Printer.default))
        .through(parseRows)
        .compile
        .toList
        .unsafeRunSync must_=== List(a)
    }.set(minTestsOk = 20, workers = 2)

    "rows should round trip" in prop { a: CSV.Rows =>
      val decoded = CSV.Rows(
        Stream
          .emits[IO, CSV.Row](a.rows)
          .through(encodeRows(Printer.default))
          .through(parseRows)
          .compile
          .toList
          .unsafeRunSync
      )
      decoded must_=== a
    }.set(minTestsOk = 20, workers = 2)

    "complete should round trip " in {
      case class Foo(color: String, food: String, number: Int)

      val list = List(
        Foo("Blue", "Pizza", 1),
        Foo("Red", "Margarine", 2),
        Foo("Yellow", "Broccoli", 3)
      )

      implicit val L: LabelledWrite[Foo] = new LabelledWrite[Foo] {
        override def headers: CSV.Headers = CSV.Headers(
          NonEmptyList.of(CSV.Header("Color"), CSV.Header("Food"), CSV.Header("Number"))
        )

        override def write(a: Foo): CSV.Row = CSV.Row(
          NonEmptyList.of(a.color.field, a.food.field, a.number.field)
        )
      }

      val result = Stream.emits(list)
        .through(writeLabelled(Printer.default))
        .compile
        .string

      val expectedCSVString = """Color,Food,Number
                                |Blue,Pizza,1
                                |Red,Margarine,2
                                |Yellow,Broccoli,3""".stripMargin

      result should_=== expectedCSVString
    }

  }

}

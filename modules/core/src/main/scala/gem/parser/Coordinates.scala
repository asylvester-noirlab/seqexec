// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.parser

import cats.implicits._
import atto._, Atto._
import gem.math._

/** Parsers for [[gem.math.Coordinates]] and related types. */
trait CoordinateParsers {
  import AngleParsers.{ hms, dms }
  import MiscParsers.spaces1

  /** Parser for a RightAscension, always a positive angle in HMS. */
  val ra: Parser[RightAscension] =
    hms.map(RightAscension(_))

  /** Parser for a RightAscension, always a positive angle in HMS. */
  val dec: Parser[Declination] =
    dms.map(Declination.fromAngle).flatMap {
      case Some(ra) => ok(ra)
      case None     => err("Invalid Declination")
    }

  /** Parser for coordinates: HMS and DMS separated by spaces. */
  val coordinates: Parser[Coordinates] =
    (ra <~ spaces1, dec).mapN(Coordinates(_, _))

}
object CoordinateParsers extends CoordinateParsers

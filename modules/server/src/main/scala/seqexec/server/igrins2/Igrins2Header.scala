// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.server.igrins2

import cats.Applicative
import seqexec.model.Observation
import seqexec.model.dhs.ImageFileId
import seqexec.server.keywords._

object Igrins2Header {

  def header[F[_]: Applicative]: Header[F] =
    new Header[F] {
      override def sendBefore(obsId: Observation.Id, id: ImageFileId): F[Unit] =
        Applicative[F].unit

      override def sendAfter(id: ImageFileId): F[Unit] =
        Applicative[F].unit
    }
}

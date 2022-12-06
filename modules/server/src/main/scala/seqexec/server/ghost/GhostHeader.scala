// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.server.ghost

import cats.Applicative
import cats.MonadThrow
import cats.syntax.all._
import seqexec.model.Observation
import seqexec.model.dhs.ImageFileId
import seqexec.server.keywords._
import lucuma.core.enums.KeywordName
import seqexec.server.tcs.TcsKeywordsReader

object GhostHeader {
  val bool2String: Boolean => String = (v: Boolean) => if (v) "T" else "F"

  def header[F[_]: MonadThrow](
    gdsClient:           GdsClient[F],
    tcsKeywordsReader:   TcsKeywordsReader[F],
    ghostKeywordsReader: GhostKeywordsReader[F]
  ): Header[F] =
    new Header[F] {
      override def sendBefore(obsId: Observation.Id, id: ImageFileId): F[Unit] = {
        val ks = GdsInstrument.bundleKeywords[F](
          List(
            buildString(ghostKeywordsReader.basePos.map(bool2String), KeywordName.BASEPO),
            buildInt32(tcsKeywordsReader.ghostInstPort, KeywordName.INPORT),
            buildString(ghostKeywordsReader.srifu1, KeywordName.SRIFU1),
            buildString(ghostKeywordsReader.srifu2, KeywordName.SRIFU2),
            buildString(ghostKeywordsReader.hrifu1, KeywordName.HRIFU1),
            buildString(ghostKeywordsReader.hrifu2, KeywordName.HRIFU2),
            buildString(ghostKeywordsReader.fiberAgitator1Enabled.map(bool2String),
                        KeywordName.FAGITAT1
            ),
            buildString(ghostKeywordsReader.fiberAgitator2Enabled.map(bool2String),
                        KeywordName.FAGITAT2
            ),
            buildString(ghostKeywordsReader.ifu1Guiding.map(bool2String), KeywordName.IFU1GUID),
            buildString(ghostKeywordsReader.ifu2Guiding.map(bool2String), KeywordName.IFU2GUID),
            buildInt32(ghostKeywordsReader.blueCount.orDefault, KeywordName.NBLUEEXP),
            buildDouble(ghostKeywordsReader.blueDuration.orDefault, KeywordName.BLUEEXPT),
            buildInt32(ghostKeywordsReader.redCount.orDefault, KeywordName.NREDEXP),
            buildDouble(ghostKeywordsReader.redDuration.orDefault, KeywordName.REDEXPT),
            buildString(ghostKeywordsReader.redCcds.orDefault, KeywordName.REDCCDS),
            buildString(ghostKeywordsReader.blueCcds.orDefault, KeywordName.BLUECCDS),
            buildString(ghostKeywordsReader.redReadMode.orDefault, KeywordName.READRED),
            buildString(ghostKeywordsReader.blueReadMode.orDefault, KeywordName.READBLUE)
            // buildInt32(ghostKeywordsReader.slitCount.orDefault, KeywordName.NSLITEXP),
            // buildDouble(ghostKeywordsReader.slitDuration.orDefault, KeywordName.SLITEXPT),
          )
        )
        ks.flatMap(gdsClient.openObservation(obsId, id, _))
      }

      override def sendAfter(id: ImageFileId): F[Unit] =
        Applicative[F].unit
    }
}

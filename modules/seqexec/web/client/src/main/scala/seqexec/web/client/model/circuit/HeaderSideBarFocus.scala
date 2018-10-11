// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package seqexec.web.client.circuit

import cats.Eq
import cats.implicits._
import gem.Observation
import monocle.Getter
import monocle.macros.Lenses
import seqexec.model._
import seqexec.model.enum.Instrument
import seqexec.web.client.model._

final case class SequenceObserverFocus(instrument: Instrument,
                                       obsId:      Observation.Id,
                                       completed:  Boolean,
                                       observer:   Option[Observer])

object SequenceObserverFocus {
  implicit val eq: Eq[SequenceObserverFocus] =
    Eq.by(x => (x.instrument, x.obsId, x.completed, x.observer))

}

@Lenses
final case class HeaderSideBarFocus(
  status:     ClientStatus,
  conditions: Conditions,
  operator:   Option[Operator],
  observer:   Either[Observer, SequenceObserverFocus])

@SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
object HeaderSideBarFocus {
  implicit val eq: Eq[HeaderSideBarFocus] =
    Eq.by(x => (x.status, x.conditions, x.operator, x.observer))

  val headerSideBarG: Getter[SeqexecAppRootModel, HeaderSideBarFocus] =
    Getter[SeqexecAppRootModel, HeaderSideBarFocus] { c =>
      val clientStatus = ClientStatus(c.uiModel.user, c.ws)
      val obs = c.uiModel.sequencesOnDisplay.selectedOperator
        .toRight(c.uiModel.defaultObserver)
      HeaderSideBarFocus(clientStatus,
                         c.sequences.conditions,
                         c.sequences.operator,
                         obs)
    }
}
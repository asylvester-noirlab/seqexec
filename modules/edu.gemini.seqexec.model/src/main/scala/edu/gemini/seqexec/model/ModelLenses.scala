// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.seqexec.model

import monocle.{Lens, Optional, Prism, Traversal}
import monocle.macros.{GenLens, GenPrism}
import monocle.function.At.atMap
import monocle.function.At.at
import monocle.std.option.some
import monocle.Iso

import scalaz.Applicative
import scalaz.std.list._
import scalaz.std.anyVal._
import scalaz.std.map._
import scalaz.syntax.std.string._
import scalaz.syntax.equal._
import scalaz.syntax.show._
import scalaz.syntax.applicative._
import scalaz.syntax.traverse._

trait ModelLenses {
  import Model._
  import Model.SeqexecEvent._

    // Some useful Monocle lenses
  val obsNameL: Lens[SequenceView, String] = GenLens[SequenceView](_.metadata.name)
  // From step to standard step
  val standardStepP: Prism[Step, StandardStep] = GenPrism[Step, StandardStep]
  val eachStepT: Traversal[List[Step], Step] = Traversal.fromTraverse[List, Step]
  val obsStepsL: Lens[SequenceView, List[Step]] = GenLens[SequenceView](_.steps)
  val eachViewT: Traversal[List[SequenceView], SequenceView] = Traversal.fromTraverse[List, SequenceView]
  val sequencesQueueL: Lens[SequencesQueue[SequenceView], List[SequenceView]] = GenLens[SequencesQueue[SequenceView]](_.queue)
  // from standard step to config
  val stepConfigL: Lens[StandardStep, StepConfig] = GenLens[StandardStep](_.config)
  // Prism to focus on only the SeqexecEvents that have a queue
  val sequenceEventsP: Prism[SeqexecEvent, SeqexecModelUpdate] = GenPrism[SeqexecEvent, SeqexecModelUpdate]
  // Required for type correctness
  val stepConfigRoot: Iso[Map[SystemName, Parameters], Map[SystemName, Parameters]] = Iso.id[Map[SystemName, Parameters]]
  val parametersRoot: Iso[Map[ParamName, ParamValue], Map[ParamName, ParamValue]] = Iso.id[Map[ParamName, ParamValue]]

  // Focus on a param value
  def paramValueL(param: ParamName): Lens[Parameters, Option[String]] =
    parametersRoot ^|-> // map of parameters
    at(param)           // parameter containing the name

  // Possible set of observe parameters
  def systemConfigL(system: SystemName): Lens[StepConfig, Option[Parameters]] =
    stepConfigRoot ^|-> // map of systems
    at(system)          // subsystem name

  // Param name of a StepConfig
  def configParamValueO(system: SystemName, param: String): Optional[StepConfig, String] =
    systemConfigL(system)                ^<-? // observe paramaters
    some                                 ^|-> // focus on the option
    paramValueL(system.withParam(param)) ^<-? // find the target name
    some                                      // focus on the option

  // Focus on the sequence view
  val sequenceQueueViewL: Lens[SeqexecModelUpdate, SequencesQueue[SequenceView]] = Lens[SeqexecModelUpdate, SequencesQueue[SequenceView]](_.view)(q => {
      case e @ SequenceStart(_)           => e.copy(view = q)
      case e @ StepExecuted(_)            => e.copy(view = q)
      case e @ FileIdStepExecuted(_, _)   => e.copy(view = q)
      case e @ SequenceCompleted(_)       => e.copy(view = q)
      case e @ SequenceLoaded(_, v)       => e.copy(view = q)
      case e @ SequenceUnloaded(_, v)     => e.copy(view = q)
      case e @ StepBreakpointChanged(_)   => e.copy(view = q)
      case e @ OperatorUpdated(_)         => e.copy(view = q)
      case e @ ObserverUpdated(_)         => e.copy(view = q)
      case e @ ConditionsUpdated(_)       => e.copy(view = q)
      case e @ StepSkipMarkChanged(_)     => e.copy(view = q)
      case e @ SequencePauseRequested(_)  => e.copy(view = q)
      case e @ SequencePauseCanceled(_)   => e.copy(view = q)
      case e @ SequenceRefreshed(_)       => e.copy(view = q)
      case e @ ActionStopRequested(_)     => e.copy(view = q)
      case e @ ResourcesBusy(_, _)        => e.copy(view = q)
      case e @ SequenceUpdated(_)         => e.copy(view = q)
      case e                              => e
    }
  )
  // Composite lens to change the sequence name of an event
  val sequenceNameT: Traversal[SeqexecEvent, ObservationName] =
    sequenceEventsP         ^|->  // Events with model updates
    sequenceQueueViewL         ^|->  // Find the sequence view
    sequencesQueueL ^|->> // Find the queue
    eachViewT       ^|->  // each sequence on the queue
    obsNameL              // sequence's observation name

  // Composite lens to find the step config
  val sequenceConfigT: Traversal[SeqexecEvent, StepConfig] =
    sequenceEventsP           ^|->  // Events with model updates
    sequenceQueueViewL           ^|->  // Find the sequence view
    sequencesQueueL   ^|->> // Find the queue
    eachViewT         ^|->  // each sequence on the queue
    obsStepsL         ^|->> // sequence steps
    eachStepT         ^<-?  // each step
    standardStepP     ^|->  // which is a standard step
    stepConfigL             // configuration of the step

  def filterEntry[K, V](predicate: (K, V) => Boolean): Traversal[Map[K, V], V] =
    new Traversal[Map[K, V], V]{
      def modifyF[F[_]: Applicative](f: V => F[V])(s: Map[K, V]): F[Map[K, V]] =
        s.map { case (k, v) =>
          k -> (if(predicate(k, v)) f(v) else v.pure[F])
        }.sequenceU
    }

  // Find the Parameters of the steps containing science steps
  val scienceStepT: Traversal[StepConfig, Parameters] = filterEntry[SystemName, Parameters] {
    case (s, p) => s === SystemName.observe && p.exists {
      case (k, v) => k === SystemName.observe.withParam("observeType") && v === "OBJECT"
    }
  }

  val scienceTargetNameO: Optional[Parameters, TargetName] =
    paramValueL(SystemName.observe.withParam("object")) ^<-? // find the target name
    some                                                     // focus on the option

  val stringToStepTypeP: Prism[String, StepType] = Prism(StepType.fromString)(_.shows)
  private[model] def telescopeOffsetPI: Iso[Double, TelescopeOffset.P] = Iso(TelescopeOffset.P.apply)(_.value)
  private[model] def telescopeOffsetQI: Iso[Double, TelescopeOffset.Q] = Iso(TelescopeOffset.Q.apply)(_.value)
  val stringToDoubleP: Prism[String, Double] = Prism((x: String) => x.parseDouble.toOption)(_.shows)

  val stepTypeO: Optional[Step, StepType] =
    standardStepP                                            ^|-> // which is a standard step
    stepConfigL                                              ^|-> // configuration of the step
    systemConfigL(SystemName.observe)                        ^<-? // Observe config
    some                                                     ^|-> // some
    paramValueL(SystemName.observe.withParam("observeType")) ^<-? // find the target name
    some                                                     ^<-? // focus on the option
    stringToStepTypeP                                             // step type

  // Lens to find p offset
  def telescopeOffsetO(x: OffsetAxis): Optional[Step, Double] =
    standardStepP                                             ^|-> // which is a standard step
    stepConfigL                                               ^|-> // configuration of the step
    systemConfigL(SystemName.telescope)                       ^<-? // Observe config
    some                                                      ^|-> // some
    paramValueL(SystemName.telescope.withParam(x.configItem)) ^<-? // find the offset
    some                                                      ^<-? // focus on the option
    stringToDoubleP                                                // double value

  val telescopeOffsetPO: Optional[Step, TelescopeOffset.P] = telescopeOffsetO(OffsetAxis.AxisP) ^<-> telescopeOffsetPI
  val telescopeOffsetQO: Optional[Step, TelescopeOffset.Q] = telescopeOffsetO(OffsetAxis.AxisQ) ^<-> telescopeOffsetQI

  // Composite lens to find the step config
  val firstScienceTargetNameT: Traversal[SeqexecEvent, TargetName] =
    sequenceConfigT     ^|->> // sequence configuration
    scienceStepT        ^|-?  // science steps
    scienceTargetNameO        // science target name

  // Composite lens to find the target name on observation
  val observeTargetNameT: Traversal[SeqexecEvent, TargetName] =
    sequenceConfigT                                 ^|-?  // configuration of the step
    configParamValueO(SystemName.observe, "object")       // on the configuration find the target name

  // Composite lens to find the target name on telescope
  val telescopeTargetNameT: Traversal[SeqexecEvent, TargetName] =
    sequenceConfigT                                       ^|-?  // configuration of the step
    configParamValueO(SystemName.telescope, "Base:name")        // on the configuration find the target name


  // Composite lens to find the first science step and from there the target name
  val firstScienceStepTargetNameT: Traversal[SequenceView, TargetName] =
    obsStepsL           ^|->> // observation steps
    eachStepT           ^<-?  // each step
    standardStepP       ^|->  // only standard steps
    stepConfigL         ^|->> // get step config
    scienceStepT        ^|-?  // science steps
    scienceTargetNameO        // science target name

}
package edu.gemini.seqexec.web.client.components

import diode.react.ModelProxy
import edu.gemini.seqexec.model.Model.SequenceState
import edu.gemini.seqexec.web.client.model._
import edu.gemini.seqexec.web.client.model.Pages._
import edu.gemini.seqexec.web.client.model.ModelOps._
import edu.gemini.seqexec.web.client.semanticui.elements.icon.Icon.{IconAttention, IconCheckmark, IconCircleNotched, IconSelectedRadio}
import edu.gemini.seqexec.web.client.semanticui.elements.table.TableHeader
import edu.gemini.seqexec.web.client.services.HtmlConstants.{iconEmpty, nbsp}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.Unmounted
import org.scalajs.dom.html.TableRow

import scalacss.ScalaCssReact._
import scalaz.syntax.show._
import scalaz.syntax.equal._
import scalaz.syntax.std.option._

object QueueTableBody {
  type SequencesModel = ModelProxy[StatusAndLoadedSequences]

  case class Props(sequences: SequencesModel)

  // Minimum rows to display, pad with empty rows if needed
  val minRows = 5

  def emptyRow(k: String, isLogged: Boolean): VdomTagOf[TableRow] = {
    <.tr(
      ^.key := k, // React requires unique keys
      <.td(
        ^.cls := "collapsing",
        iconEmpty),
      <.td(nbsp),
      <.td(nbsp),
      <.td(nbsp),
      <.td(
        SeqexecStyles.notInMobile,
        nbsp).when(isLogged)
    )
  }

  def showSequence(p: Props, s: SequenceInQueue): Callback =
    // Request to display the selected sequence
    p.sequences.dispatchCB(NavigateTo(InstrumentPage(s.instrument, s.id.some))) >> p.sequences.dispatchCB(SelectIdToDisplay(s.id))

  private val component = ScalaComponent.builder[Props]("QueueTableBody")
    .render_P { p =>
      val (isLogged, sequences) = (p.sequences().isLogged, p.sequences().sequences)
      <.table(
        ^.cls := "ui selectable compact celled table unstackable",
        <.thead(
          <.tr(
            SeqexecStyles.notInMobile,
            TableHeader(TableHeader.Props(collapsing = true),  iconEmpty),
            TableHeader("Obs ID"),
            TableHeader("State"),
            TableHeader("Instrument"),
            TableHeader("Obs. Name").when(isLogged)
          )
        ),
        <.tbody(
          sequences.map(Some.apply).padTo(minRows, None).zipWithIndex.collect {
            case (Some(s), i) =>
              <.tr(
                ^.classSet(
                  "positive" -> (s.status === SequenceState.Completed),
                  "warning"  -> (s.status === SequenceState.Running),
                  "negative" -> s.status.hasError,
                  "active"   -> s.active
                ),
                ^.key := s"item.queue.$i",
                ^.onClick --> showSequence(p, s),
                <.td(
                  ^.cls := "collapsing",
                  s.status match {
                    case SequenceState.Completed                   => IconCheckmark
                    case SequenceState.Running                     => IconCircleNotched.copy(IconCircleNotched.p.copy(loading = true))
                    case SequenceState.Error(_)                    => IconAttention
                    case _                                         => if (s.active) IconSelectedRadio else iconEmpty
                  }
                ),
                <.td(
                  ^.cls := "collapsing",
                  s.id
                ),
                <.td(
                  s.status.shows + s.runningStep.map(u => s" ${u._1 + 1}/${u._2}").getOrElse("")
                ),
                <.td(
                  s.instrument
                ),
                <.td(
                  SeqexecStyles.notInMobile,
                  s.name
                ).when(isLogged)
              )
            case (_, i) =>
              emptyRow(s"item.queue.$i", isLogged)
          }.toTagMod
        )
      )
    }
    .build

  def apply(p: SequencesModel): Unmounted[Props, Unit, Unit] = component(Props(p))

}

/**
  * Container for the queue table
  */
object QueueTableSection {
  private val sequencesConnect = SeqexecCircuit.connect(SeqexecCircuit.statusAndLoadedSequences)

  private val component = ScalaComponent.builder[Unit]("QueueTableSection")
    .stateless
    .render_P(p =>
      <.div(
        ^.cls := "ui segment scroll pane",
        SeqexecStyles.queueListPane,
        sequencesConnect(QueueTableBody.apply)
      )
    ).build

  def apply(): Unmounted[Unit, Unit, Unit] = component()

}

/**
  * Displays the elements on the queue
  */
object QueueArea {

  private val component = ScalaComponent.builder[Unit]("QueueArea")
    .stateless
    .render_P(p =>
      <.div(
        ^.cls := "ui raised segments container",
        TextMenuSegment("Night Queue", "key.queue.menu"),
        <.div(
          ^.cls := "ui attached segment",
          <.div(
            ^.cls := "ui grid",
            <.div(
              ^.cls := "stretched row",
              <.div(
                ^.cls := "sixteen wide column",
                QueueTableSection()
              )
            )
          )
        )
      )
    )
    .build

  def apply(): Unmounted[Unit, Unit, Unit] = component()

}

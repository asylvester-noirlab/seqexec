// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.seqexec.web.client.components.sequence.steps

import scala.scalajs.js
import diode.react.ModelProxy
import edu.gemini.seqexec.model.Model.{Step}
// import edu.gemini.seqexec.web.client.ModelOps._
// import edu.gemini.seqexec.web.client.model.Pages.{SeqexecPages, SequenceConfigPage}
import edu.gemini.seqexec.web.client.model.Pages.SeqexecPages
// import edu.gemini.seqexec.web.client.actions.{FlipBreakpointStep, FlipSkipStep, NavigateSilentTo}
// import edu.gemini.seqexec.web.client.circuit.{ClientStatus, SeqexecCircuit, StepsTableFocus}
import edu.gemini.seqexec.web.client.circuit.{ClientStatus, StepsTableFocus}
import edu.gemini.seqexec.web.client.components.SeqexecStyles
  import edu.gemini.seqexec.web.client.components.sequence.steps.OffsetFns._
// import edu.gemini.seqexec.web.client.lenses.stepTypeO
// import edu.gemini.seqexec.web.client.semanticui._
// import edu.gemini.seqexec.web.client.semanticui.elements.icon.Icon
// import edu.gemini.seqexec.web.client.semanticui.elements.icon.Icon._
// import edu.gemini.seqexec.web.client.semanticui.elements.label.Label
// import edu.gemini.seqexec.web.client.semanticui.elements.message.IconMessage
// import edu.gemini.seqexec.web.client.services.HtmlConstants.iconEmpty
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
// import org.scalajs.dom.html.Div
//
import scalacss.ScalaCssReact._
import scalaz.std.AllInstances._
import scalaz.syntax.foldable._
// import scalaz.syntax.show._
// import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import react.virtualized._

object ColWidths {
  val IdxWidth: Int = 50
  val StatusWidth: Int = 100
  val OffsetWidth: Int = 100
}

/**
  * Component to display the step id
  */
object StepIdCell {
  private val component = ScalaComponent.builder[Int]("StepIdCell")
    .stateless
    .render_P { p =>
      <.div(
        s"${p + 1}")
    }.build

  def apply(i: Int): Unmounted[Int, Unit, Unit] = component(i)
}

/**
  * Component to display the offsets
  */
object OffsetsDisplayCell {
  final case class Props(offsetsDisplay: OffsetsDisplay, step: Step)

  private val component = ScalaComponent.builder[Props]("OffsetsDisplayCell")
    .stateless
    .render_P { p =>
      <.div( // Column step offset
        p.offsetsDisplay match {
          case OffsetsDisplay.DisplayOffsets(offsetWidth) =>
              OffsetBlock(OffsetBlock.Props(p.step, offsetWidth))
          case _ => EmptyVdom
        }
      )
    }.build

  def apply(i: Props): Unmounted[Props, Unit, Unit] = component(i)
}

/**
  * Container for a table with the steps
  */
object StepsTable {
  // ScalaJS defined trait
  // scalastyle:off
  trait StepRow extends js.Object {
    var step: Step
  }
  // scalastyle:on
  object StepRow {
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def apply(step: Step): StepRow = {
      val p = (new js.Object).asInstanceOf[StepRow]
      p.step = step
      p
    }

    def unapply(l: StepRow): Option[(Step)] =
      Some((l.step))

    val Zero: StepRow = apply(Step.Zero)
  }

  final case class Props(router: RouterCtl[SeqexecPages], stepsTable: ModelProxy[(ClientStatus, Option[StepsTableFocus])], onStepToRun: Int => Callback) {
    def status: ClientStatus = stepsTable()._1
    def steps: Option[StepsTableFocus] = stepsTable()._2
    private val stepsList: List[Step] = ~steps.map(_.steps)
    def rowCount: Int = stepsList.length
    def rowGetter(idx: Int): StepRow = steps.flatMap(_.steps.index(idx)).fold(StepRow.Zero)(StepRow.apply)
    // Find out if offsets should be displayed
    val offsetsDisplay: OffsetsDisplay = stepsList.offsetsDisplay
  }

  val stepIdRenderer: CellRenderer[js.Object, js.Object, StepRow] = (_, _, _, row: StepRow, _) =>
    StepIdCell(row.step.id)

  def stepStatusRenderer(offsetsDisplay: OffsetsDisplay): CellRenderer[js.Object, js.Object, StepRow] = (_, _, _, row: StepRow, _) =>
    OffsetsDisplayCell(OffsetsDisplayCell.Props(offsetsDisplay, row.step))

  // Columns for the table
  private def columns(p: Props): List[Table.ColumnArg] = {
    println(p.offsetsDisplay)
    val offsetColumn =
      p.offsetsDisplay match {
        case OffsetsDisplay.DisplayOffsets(_) =>
          Column(Column.props(ColWidths.OffsetWidth, "offset", label = "Offset", disableSort = true, cellRenderer = stepStatusRenderer(p.offsetsDisplay))).some
        case _ => None
      }
      List(
        Column(Column.props(ColWidths.IdxWidth, "idx", label = "Step", disableSort = true, cellRenderer = stepIdRenderer)).some,
        offsetColumn
      ).collect { case Some(x) => x }
  }

  def stepsTable(p: Props)(size: Size): VdomNode = {
    def rowClassName(i: Int): String = ((i, p.rowGetter(i)) match {
      case (-1, _) => SeqexecStyles.headerRowStyle
      case _       => SeqexecStyles.stepRow
    }).htmlClass

    Table(
      Table.props(
        disableHeader = false,
        noRowsRenderer = () =>
          <.div(
            ^.cls := "ui center aligned segment noRows",
            ^.height := 270.px,
            "No log entries"
          ),
        overscanRowCount = SeqexecStyles.overscanRowCount,
        height = size.height.toInt,
        rowCount = p.rowCount,
        rowHeight = SeqexecStyles.rowHeight,
        rowClassName = rowClassName _,
        width = size.width.toInt,
        rowGetter = p.rowGetter _,
        headerClassName = SeqexecStyles.tableHeader.htmlClass,
        headerHeight = SeqexecStyles.headerHeight),
      columns(p): _*).vdomElement
  }

  private val component = ScalaComponent.builder[Props]("Steps")
    .render_P { p =>
      <.div(
        SeqexecStyles.stepsListPane.unless(p.status.isLogged),
        SeqexecStyles.stepsListPaneWithControls.when(p.status.isLogged),
        p.steps.whenDefined { tab =>
          tab.stepConfigDisplayed.map { i =>
            <.div("CONFIG")
          }.getOrElse {
            AutoSizer(AutoSizer.props(stepsTable(p)))
          }
        }
      )
    }.build

  def apply(p: Props): Unmounted[Props, Unit, Unit] = component(p)
}

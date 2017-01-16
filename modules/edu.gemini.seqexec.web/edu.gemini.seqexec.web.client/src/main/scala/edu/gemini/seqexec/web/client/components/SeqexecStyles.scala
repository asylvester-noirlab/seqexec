package edu.gemini.seqexec.web.client.components

import scalacss.Defaults._

/**
  * Custom CSS for the Seqexec UI
  */
object SeqexecStyles extends StyleSheet.Inline {
  import dsl._

  val body = style(unsafeRoot("body")(
    backgroundColor(white)
  ))

  val mainContainer = style(
    addClassNames("main", "ui", "borderless", "menu", "container")
  )

  val navBar = style("navbar")(
    unsafeRoot(".main.ui.borderless.menu.container.placeholder")(
      marginTop(0.px)
    )
  )

  val topLogo = style("main.menu .item img.logo")(
    marginRight(1.5.em)
  )

  // Media query to adjust the width of containers on mobile to the max allowed width
  val deviceContainer = style("ui.container")(
    media.only.screen.maxWidth(767.px)(
      width(100.%%).important,
      marginLeft(0.px).important,
      marginRight(0.px).important
    )
  )

  val scrollPane = style("ui.scroll.pane")(
    overflow.auto
  )

  val queueListPane = style {
    maxHeight(13.1.em)
  }

  val searchResultListPane = style {
    maxHeight(10.3.em)
  }

  val stepsListPane = style {
    maxHeight(24.3.em)
  }

  val stepsListBody = style() // Marker css
  val stepRunning = style() // Marker css

  val observeConfig = style {
    backgroundColor.lightcyan
  }

  val inline = style {
    display.inline
  }

  val scrollPaneSegment = style("ui.scroll.pane.segment")(
    padding(0.px),
    marginTop(0.px),
    unsafeChild("> .ui.table")(
      border(0.px),
      borderSpacing(0.px)
    )
  )

  val hidden = style(
    display.none
  )

  val tdNoPadding = style(
    padding(0.px).important
  )

  val progressVCentered = style("ui.progress.vcentered")(
    marginBottom(0.px)
  )

  // Common properties for a segment displayed when running
  val segmentRunningMixin = mixin(
    backgroundColor(rgba(0, 0, 0, 0.0)).important,
    color.inherit,
    padding(0.em),
    margin(0.px),
    (boxShadow := ("none")).important
  )

  // CSS for a segment where a step is running
  val segmentRunning = style("ui.segment.running")(
    segmentRunningMixin,
    borderLeft.none.important,
    alignSelf.center
  )

  // CSS for a segments where a step is running
  val segmentsRunning = style("ui.segments.running")(
    segmentRunningMixin,
    border.none,
    borderRadius(0.px)
  )

  // Media queries to hide/display items for mobile
  val notInMobile = style(
    media.only.screen.maxWidth(767.px)(
      display.none.important
    )
  )
  val onlyMobile = style(
    media.only.screen.minWidth(767.px)(
      display.none.important
    )
  )

  val errorText = style(
    color.red
  )

  val smallTextArea = style(
    fontSize.smaller
  )

  val breakpointTrOff = style(
    maxHeight(0.px),
    overflow.hidden
  )

  val breakpointTrOn = style(
    height(3.px),
    backgroundColor(brown),
    border(2.px, red)
  )

}

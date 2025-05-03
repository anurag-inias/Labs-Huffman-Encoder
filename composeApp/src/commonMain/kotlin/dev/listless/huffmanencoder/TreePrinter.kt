package dev.listless.huffmanencoder

interface PrintableNode {

  fun content(): String

  fun children(): List<PrintableNode>

  fun leaf(): Boolean = children().isEmpty()

  fun left(): IntRange = 0..<children().size/2

  fun below(): Int? = with(children().size) {
    when (this % 2) {
      0 -> null
      else -> this / 2
    }
  }

  fun right(): IntRange = with(children().size) {
    when (this % 2) {
      0 -> this / 2..<this
      else -> (this / 2 + 1)..<this
    }
  }
}

fun print(node: PrintableNode): String {
  return printSubtree(node).rows.joinToString(separator = "\n")
}

private fun printSubtree(node: PrintableNode): PrintedSubtree {
  if (node.leaf()) {
    return PrintedSubtree(
      plumbAt = node.width / 2,
      rows = mutableListOf(StringBuilder(node.content()))
    )
  }

  // Step 1. Recursively create subtrees.
  // Each subtree column has even width from top to bottom.
  val subtrees = mutableListOf<PrintedSubtree>()
  for (child in node.children()) {
    subtrees.add(printSubtree(child))
  }

  // Step 2. Add column gutter.
  // All but the rightmost subtree is given a right padding.
  addColumnGutter(subtrees)

  // Step 3. Ensure that all subtree columns are same height.
  // This is useful before they can be stitched together.
  enforceMinimumHeight(subtrees)

  // Step 4. Stitch subtree columns together.
  // And adds a new row at the top with the plumbing set up.
  val merged = mergeColumns(node.content().length, subtrees, node.left(), node.below(), node.right())

  val rootRow = StringBuilder()
  val n = node.content().length
  val w = merged.width
  val leftPadding = merged.plumbAt - n / 2
  val rightPadding = w - merged.plumbAt - 1 - (n - n / 2)

  if (leftPadding < 0) {
    padding(-leftPadding, right = 0, merged)
  }
  rootRow
    .append("$SPACE".repeat(merged.plumbAt - n / 2))
    .append(node.content())

  if (rightPadding < 0) {
    rootRow.append("$SPACE".repeat(-rightPadding))
    padding(left = 0, -rightPadding, merged)
  } else {
    rootRow.append("$SPACE".repeat(rightPadding))
  }

  merged.rows.add(0, rootRow)
  trim(merged)
  return PrintedSubtree(merged.plumbAt, merged.rows)
}

private fun padding(left: Int, right: Int, subtree: PrintedSubtree) {
  for ((r, row) in subtree.rows.withIndex()) {
    subtree.plumbAt += left
    subtree.rows[r] = StringBuilder("$SPACE".repeat(left))
      .append(row)
      .append("$SPACE".repeat(right))
  }
}

private fun trim(subtree: PrintedSubtree) {
  val start = subtree.rows.minOf { row -> row.indexOfFirst { it != SPACE } }
  val end = subtree.rows.maxOf { row -> row.indexOfLast { it != SPACE } }
  if (start < 0) return
  for ((r, row) in subtree.rows.withIndex()) {
    val offset = end - start + 1 - row.length
    if (offset > 0) {
      row.append("$SPACE".repeat(offset))
    }
    subtree.rows[r] = StringBuilder(row.subSequence(start..end))
  }
}

private fun columnsPlumbing(parentWidth: Int, subtrees: List<PrintedSubtree>, left: IntRange, below: Int?, right: IntRange): StringBuilder {
  val columns = subtrees.size
  val row = StringBuilder()

  for (index in left) {
    val column = subtrees[index]
    val w = column.width
    row
      .append((if (columns > 1 && index > 0) "$DASH" else "$SPACE").repeat(column.plumbAt))
      .append((if (columns > 1 && index > 0) M_JOINT else L_JOINT))
      .append("$DASH".repeat(w - column.plumbAt - 1))
  }
  if (below == null) {
    if (left.isEmpty()) {
      row
        .append("$SPACE".repeat(parentWidth/2))
        .append(R_ONLY_JOINT)
        .append("$DASH".repeat(parentWidth - 1 - parentWidth / 2))
    } else if (right.isEmpty()) {
      row
        .append("$DASH".repeat(parentWidth/2))
        .append(L_ONLY_JOINT)
        .append("$SPACE".repeat(parentWidth - 1 - parentWidth / 2))
    } else {
      row
        .append("$DASH".repeat(parentWidth/2))
        .append(L_AND_R_JOINT)
        .append("$DASH".repeat(parentWidth - 1 - parentWidth / 2))
    }
  } else {
    val column = subtrees[below]
    var padding = parentWidth - column.width
    if (padding < 0) padding = 0
    if (left.isEmpty() && right.isEmpty()) {
      row
        .append("$SPACE".repeat(column.plumbAt + padding / 2))
        .append(B_ONLY_JOINT)
        .append("$SPACE".repeat(column.width - column.plumbAt - 1 + padding - (padding / 2)))
    } else if (left.isEmpty()) {
      row
        .append("$SPACE".repeat(column.plumbAt + padding / 2))
        .append(B_AND_R_JOINT)
        .append("$DASH".repeat(column.width - column.plumbAt - 1 + padding - (padding / 2)))
    } else if (right.isEmpty()) {
      row
        .append("$DASH".repeat(column.plumbAt + padding / 2))
        .append(B_AND_L_JOINT)
        .append("$SPACE".repeat(column.width - column.plumbAt - 1 + padding - (padding / 2)))
    } else {
      row
        .append("$DASH".repeat(column.plumbAt + padding / 2))
        .append(ALL_JOINT)
        .append("$DASH".repeat(column.width - column.plumbAt - 1 + padding - (padding / 2)))
    }
  }
  for (index in right) {
    val column = subtrees[index]
    val w = column.width
    row
      .append("$DASH".repeat(column.plumbAt))
      .append((if (columns > 1 && index < columns - 1) M_JOINT else R_JOINT))
      .append((if (columns > 1 && index < columns - 1) "$DASH" else "$SPACE").repeat(w - column.plumbAt - 1))
  }
  return row
}

// c1 c2  c3    c1 c2  c3
// aa bbb cc    aa|bbb|cc
// aa bbb cc => aa|bbb|cc
// aa     cc    aa|    cc
//        cc           cc
private fun addColumnGutter(subtrees: List<PrintedSubtree>) {
  for ((c, column) in subtrees.withIndex()) {
    if (c == subtrees.size - 1) continue
    for (row in column.rows) {
      row.append(SPACE)
    }
  }
}

// c1 c2  c3    c1 c2  c3
// aa|bbb|cc    aa|bbb|cc
// aa|bbb|cc => aa|bbb|cc
// aa|    cc    aa|•••|cc
//        cc    ••|•••|cc
private fun enforceMinimumHeight(subtrees: List<PrintedSubtree>) {
  val maxHeight = subtrees.maxOf { it.height }
  for (subtree in subtrees) {
    subtree.height = maxHeight
  }
}

// c1 c2  c3       c1
// aa|bbb|cc    aa|bbb|cc
// aa|bbb|cc => aa|bbb|cc
// aa|•••|cc    aa|•••|cc
// ••|•••|cc    ••|•••|cc
private fun mergeColumns(parentWidth: Int, subtrees: List<PrintedSubtree>, left: IntRange, below: Int?, right: IntRange): PrintedSubtree {
  val rows = mutableListOf(columnsPlumbing(parentWidth, subtrees, left, below, right))

  var plumbAt: Int? = null
  var width = 0
  val height = subtrees.first().height

  for (r in 0..<height) {
    val row = StringBuilder()
    for (i in left) {
      row.append(subtrees[i].rows[r])
      width += subtrees[i].width
    }
    if (below == null) {
      row.append("$SPACE".repeat(parentWidth))
      if (plumbAt == null) plumbAt = width + parentWidth / 2
    } else {
      val padding = parentWidth - subtrees[below].width
      if (padding <= 0) {
        row.append(subtrees[below].rows[r])
        if (plumbAt == null) plumbAt = width + subtrees[below].plumbAt
      } else {
        row
          .append("$SPACE".repeat( padding / 2))
          .append(subtrees[below].rows[r])
          .append("$SPACE".repeat(padding - padding / 2))
        if (plumbAt == null) plumbAt = width + (padding / 2) + subtrees[below].plumbAt
      }
    }
    for (i in right) {
      row.append(subtrees[i].rows[r])
      // Don't need width adjustment here.
    }
    rows.add(row)
  }

  return PrintedSubtree(plumbAt!!, rows)
}

private data class PrintedSubtree(
  var plumbAt: Int = -1,
  val rows: MutableList<StringBuilder> = mutableListOf()
) {
  val width: Int
    get() = rows.firstOrNull()?.length ?: 0

  var height: Int
    get() = rows.size
    set(value) {
      while (rows.size < value) {
        rows.add(StringBuilder("$SPACE".repeat(width)))
      }
    }
}

private val PrintableNode.width: Int
  get() = content().length

private const val SPACE = ' '
// ─ causes alignment issues when rendered.
private const val DASH = '─'
private const val L_JOINT = '┌'
private const val R_JOINT = '┐'
private const val M_JOINT = '┬'
private const val L_ONLY_JOINT = '┘'
private const val R_ONLY_JOINT = '└'
private const val B_ONLY_JOINT = '|'
private const val L_AND_R_JOINT = '┴'
private const val B_AND_R_JOINT = '├'
private const val B_AND_L_JOINT = '┤'
private const val ALL_JOINT = '┼'

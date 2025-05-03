package dev.listless.huffmanencoder

data class FrequencyNode(val frequency: Int, val token: String? = null): PrintableNode {
  var first: FrequencyNode? = null
  var second: FrequencyNode? = null
  var encoding: String = ""

  override fun content(): String {
    if (encoding.isEmpty()) return "○" // root node
    if (token != null) return "$token -> $encoding"
    return "($encoding)"
  }

  override fun children(): List<PrintableNode> {
    val children = mutableListOf<FrequencyNode>()
    if (first != null) children.add(first!!)
    if (second != null) children.add(second!!)
    return children
  }

  override fun left(): IntRange {
    return if (first == null) IntRange.EMPTY else 0..<1
  }

  override fun below(): Int? = null

  override fun right(): IntRange {
    return if (second == null) IntRange.EMPTY else 1..<2
  }

  val isLeaf: Boolean
    get() = first == null && second == null
}

class MinHeap {
  val list = ArrayList<FrequencyNode>()

  val size: Int
    get() = list.size

  fun offer(frequency: Int, token: String) {
    list.add(FrequencyNode(frequency, token))
    swim(list.size - 1)
  }

  fun offer(node: FrequencyNode) {
    list.add(node)
    swim(list.size - 1)
  }

  fun pop(): FrequencyNode {
    if (size == 0) throw NullPointerException("MinHeap empty")

    swap(0, list.size - 1)
    val top = list.removeLast()
    sink(0)

    return top
  }

  private fun sink(index: Int) {
    var p = index
    while (p < list.size - 1) {
      val l = 2 * p + 1
      val r = 2 * p + 2
      var min = p

      if (l < list.size && list[l].frequency < list[min].frequency) {
        min = l
      }
      if (r < list.size && list[r].frequency < list[min].frequency) {
        min = r
      }

      if (min == p) return
      swap(min, p)
      p = min
    }
  }

  private fun swim(index: Int) {
    var i = index
    while (i > 0) {
      val parent = (i - 1) / 2
      if (list[i].frequency >= list[parent].frequency) return
      swap(i, parent)
      i = parent
    }
  }

  private fun swap(i: Int, j: Int) {
    val temp = list[i]
    list[i] = list[j]
    list[j] = temp
  }
}
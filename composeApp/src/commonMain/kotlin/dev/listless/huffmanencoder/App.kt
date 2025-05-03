package dev.listless.huffmanencoder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import huffmanencoder.composeapp.generated.resources.CascadiaCode
import huffmanencoder.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val notesBackground = 0xFFFFF8B8

private val paddingSmall = 8.dp
private val paddingMedium = 16.dp
private val maxWidth = 420.dp

@Composable
@Preview
fun App() {
  MaterialTheme {
    var message by remember { mutableStateOf("Lorem ipsum dolor sit amet, consectetur adipiscing elit") }
    var tokenLevel by remember { mutableStateOf(TokenLevel.WORD) }
    var root by remember {
      mutableStateOf(
        buildTree(
          calculateFrequency(
            message,
            tokenLevel
          )
        )
      )
    }
    var encoded by remember {
      mutableStateOf(
        encode(
          message,
          tokenLevel,
          buildDictionary(root)
        )
      )
    }

    fun onEachChange() {
      val freq = calculateFrequency(message, tokenLevel)
      val tree = buildTree(freq)
      val dictionary = buildDictionary(tree)
      root = tree
      encoded = encode(message, tokenLevel, dictionary)
    }

    LazyColumn(
      modifier = Modifier.padding(all = paddingMedium).fillMaxHeight()
        .widthIn(min = 0.dp, max = maxWidth),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item {
        TextField(
          label = { Text("Message to be encoded") },
          value = message,
          onValueChange = {
            message = it
          },
          modifier = Modifier.fillMaxWidth(),
          minLines = 8,
          maxLines = Int.MAX_VALUE,
        )
      }
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.Start,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().clickable(
              onClick = {
                tokenLevel = TokenLevel.RUNE
              }
            ),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = tokenLevel == TokenLevel.RUNE,
              onClick = {},
            )
            Text("Token level: Character")
          }
          Row(
            modifier = Modifier.fillMaxWidth().clickable(
              onClick = {
                tokenLevel = TokenLevel.WORD
              }
            ),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
              selected = tokenLevel == TokenLevel.WORD,
              onClick = {},
            )
            Text("Token level: Word")
          }
        }
      }
      item {
        Button(onClick = { onEachChange() }, content = { Text("Encode") })
      }
      item {
        TextField(
          label = { Text("Encoded message") },
          value = encoded,
          onValueChange = {},
          modifier = Modifier.fillMaxWidth(),
          minLines = 8,
          maxLines = Int.MAX_VALUE,
        )
      }
      item {
        FrequencyTree(root)
      }
    }
  }
}

@Composable
fun FrequencyTree(root: FrequencyNode) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = paddingMedium)) {
    SelectionContainer {
      Text(
        modifier = Modifier.fillMaxWidth()
          .background(color = Color(notesBackground))
          .padding(all = paddingSmall)
          .horizontalScroll(rememberScrollState()),
        style = TextStyle(
          fontFamily = FontFamily(
            Font(Res.font.CascadiaCode)
          ),
        ),
        text = print(root),
        maxLines = Int.MAX_VALUE,
      )
    }
  }
}

fun encode(
  message: String,
  tokenLevel: TokenLevel,
  dictionary: Map<String, String>
): String {
  val out = StringBuilder()

  if (tokenLevel == TokenLevel.RUNE) {
    for (c in message) {
      out.append(dictionary["$c"] ?: '�')
    }
    if (out.isEmpty()) {
      out.append(dictionary[message])
    }
  } else {
    for (t in message.split(" ")) {
      out.append(dictionary[t] ?: '�')
    }
    if (out.isEmpty()) {
      out.append(dictionary[message])
    }
  }

  return out.toString()
}

fun buildDictionary(root: FrequencyNode): Map<String, String> {
  val dict = HashMap<String, String>()

  fun visit(node: FrequencyNode?) {
    if (node == null) return
    if (node.isLeaf) {
      dict[node.token!!] = node.encoding
      return
    }
    if (node.first != null) {
      node.first!!.encoding = node.encoding + '0'
      visit(node.first)
    }
    if (node.second != null) {
      node.second!!.encoding = node.encoding + '1'
      visit(node.second)
    }
  }
  visit(root)
  if (root.isLeaf) root.encoding = "0"

  return dict
}

fun buildTree(counts: Map<String, Int>): FrequencyNode {
  val minHeap = MinHeap()
  for ((t, f) in counts) {
    minHeap.offer(f, t)
  }
  while (minHeap.size > 1) {
    val first = minHeap.pop()
    val second = minHeap.pop()
    val parent = FrequencyNode(first.frequency + second.frequency)
    parent.first = first
    parent.second = second
    minHeap.offer(parent)
  }
  return minHeap.pop()
}

enum class TokenLevel {
  RUNE,
  WORD,
}

fun calculateFrequency(
  message: String,
  tokenLevel: TokenLevel
): Map<String, Int> {
  val counts = HashMap<String, Int>()

  if (tokenLevel == TokenLevel.RUNE) {
    for (c in message) {
      counts["$c"] = (counts["$c"] ?: 0) + 1
    }
  } else {
    for (t in message.split(" ")) {
      counts[t] = (counts[t] ?: 0) + 1
    }
  }

  return counts
}

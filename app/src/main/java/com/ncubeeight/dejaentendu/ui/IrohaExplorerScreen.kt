package com.ncubeeight.dejaentendu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncubeeight.dejaentendu.studynotes.WordGlossGenerator
import kotlinx.coroutines.launch

private val IROHA_LINES: List<List<String>> = listOf(
    listOf("いろは", "にほへと"),
    listOf("ちりぬる", "を"),
    listOf("わかよ", "たれそ"),
    listOf("つね", "ならむ"),
    listOf("うゐの", "おくやま"),
    listOf("けふ", "こえて"),
    listOf("あさき", "ゆめみし"),
    listOf("ゑひも", "せす"),
)

private val IROHA_PALETTE: List<Color> = listOf(
    Color(0xFFD62929), // red
    Color(0xFFE86E0F), // orange
    Color(0xFFC48A05), // amber
    Color(0xFF178541), // green
    Color(0xFF089297), // teal
    Color(0xFF2654D1), // blue
    Color(0xFF6E29BF), // violet
    Color(0xFFBF1775), // magenta
)

private data class IrohaWord(val word: String, val line: String, val color: Color)

/**
 * Flattened so the poem wraps to fill each line naturally (FlowRow) instead
 * of being locked to a fixed 2-words-per-row grid — the couplet breaks are
 * a layout artifact, not a meaningful word boundary.
 */
private val ALL_IROHA_WORDS: List<IrohaWord> = buildList {
    var index = 0
    for (words in IROHA_LINES) {
        val line = words.joinToString("")
        for (word in words) {
            add(IrohaWord(word, line, IROHA_PALETTE[index % IROHA_PALETTE.size]))
            index++
        }
    }
}

/**
 * A tap-to-define reading practice screen: the Iroha, a classical Japanese
 * pangram poem, laid out word-by-word with each word colored distinctly so
 * word boundaries are visually obvious in unspaced hiragana. Tapping a word
 * looks up a short English gloss via the on-device model
 * (WordGlossGenerator.glossClassicalJapanese). Serves as a live demo of the
 * tap-to-look-up interaction used throughout the app, plus usage guidelines
 * for the app overall. Mirrors iOS's IrohaExplorerView.swift.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IrohaExplorerScreen() {
    val uriHandler = LocalUriHandler.current

    Scaffold(topBar = { TopAppBar(title = { Text("Example interaction") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Select a word to see the translation from your device's built-in language model",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (item in ALL_IROHA_WORDS) {
                    IrohaWordToken(item)
                }
            }

            Text(
                "Japanese Iroha poem",
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable { uriHandler.openUri("https://en.wikipedia.org/wiki/Iroha") },
            )

            UsageGuidelines()
        }
    }
}

@Composable
private fun UsageGuidelines() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Usage Guidelines:", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        Text(
            "Use the Samples tab to import an image, audio file, or sample text from somewhere on your phone of " +
                "the language you want to study. Declare the language you think the text or audio is in, to " +
                "instruct the on-device model of the target language. Once the sample has been processed on-device " +
                "by your phone's Gemini Nano language model, you can add any term on that page to a list of " +
                "Vocabulary as individual flash cards.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "When you want to review your previously seen text and previously seen audio terms later, click " +
                "directly into the Vocabulary tab and select the speak icon to have your device give you the " +
                "suggested pronunciation of the term and an example sentence. Delete any flashcards once you're " +
                "done studying.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "You can add multiple languages to your import list by selecting others in the Settings tab. A " +
                "sample in the app processes only one language at a time.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text("Thank you for using Déjà Entendu!", color = MaterialTheme.colorScheme.onBackground)
    }
}

private sealed interface IrohaGlossState {
    data object Loading : IrohaGlossState
    data class Ready(val gloss: String) : IrohaGlossState
    data class Failed(val reason: String) : IrohaGlossState
}

@Composable
private fun IrohaWordToken(item: IrohaWord) {
    var isExpanded by remember { mutableStateOf(false) }
    var glossState by remember { mutableStateOf<IrohaGlossState?>(null) }
    val scope = rememberCoroutineScope()

    Text(
        item.word,
        color = item.color,
        fontSize = 20.sp,
        modifier = Modifier
            .clickable {
                isExpanded = true
                if (glossState == null) {
                    glossState = IrohaGlossState.Loading
                    scope.launch {
                        glossState = try {
                            IrohaGlossState.Ready(WordGlossGenerator.glossClassicalJapanese(item.word, item.line))
                        } catch (e: Exception) {
                            IrohaGlossState.Failed(e.message ?: e.toString())
                        }
                    }
                }
            }
            .padding(horizontal = 3.dp, vertical = 2.dp),
    )

    DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (val current = glossState) {
                is IrohaGlossState.Loading, null -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Looking up…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                is IrohaGlossState.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(current.gloss, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text("On-device definition", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                is IrohaGlossState.Failed -> {
                    Text(current.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

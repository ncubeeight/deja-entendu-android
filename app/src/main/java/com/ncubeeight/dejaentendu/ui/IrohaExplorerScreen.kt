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
            "Thank you for using Déjà Entendu to complement your language study voyage. Please select the languages you wish to focus on in your Settings tab. You may select languages that are previously existing libraries supported by your device in the toggle list. These are capabilities of your device, not of the app itself. If your language of study is not there, but you have a downloaded dictionary on your device, you could use the “Connect Local Dictionary” selector to point the app to your preferred dictionary in your Files. Thereafter, the app will add terms from your selected dictionaries into future interpretation and flashcard generation screens. The app is not pre-populated with external dictionaries nor flashcards. The app uses user-assigned dictionaries that are already on the device instead of making queries to the web. Default options for languages are settings within Android and aren’t controlled by the developer. So any language Android doesn’t currently support must be brought into use by the user independently in Settings.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "Once your desired languages are set, you can use the Samples tab to give the app access to a file on your device which you wish to translate and practice. File type examples might include an image, pdf or audio file that you’ve created or learning materials teachers have provided to you in that specific language. Select the language of the sample when you add a new file for interpretation. You may modify Settings if your intended language isn’t showing the Samples menu until it appears there. You may also limit the languages appearing in the Samples menu by switching their toggle in the Settings tab. By assigning the language on sample processing, your device’s local text extraction will flag the sample for future flashcard creation specific to that language along with audio pronunciations when available on device. Selecting a mismatched language to the sample will potentially result in mispronunciations in the flashcards caused by the mismatching of languages to derived audio files.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "If you do not have example image or audio files, you may try to create some by choosing the “Generate Sample” in the Samples tab to have Gemini synthesize a practice phrase randomly that introduces a few terms in the language you select. You may also speak short sample sentences into the device microphone of you speaking the language you are studying. (Please do not make recordings of other people without their consent.)",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "We hope you find delight in studying new vocabulary with Gemini and Déjà Entendu. The Android developer community will be expanding new language capabilities over time as more communities build open source libraries.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text("Supplemental Notes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

        Text(
            "Please make sure on your Android device that you have enabled Gemini Nano to operate in your Settings and System. This app uses the large language model capabilities of Gemini tools for the parsing, interpretation and speech samples generated locally on your device. The app does not make outbound calls to the web-based Gemini service. So all translation, voice synthesis and flashcard generation stays locally on your device without needing to call out to the web. This means that it can be used without having a cellular signal or on Airplane Mode. If you download additional languages or dictionaries to your device please make sure to have wifi enabled when downloading those dictionaries from Android as some Android language packages can be large.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "Gemini and LLMs in general synthesize information based on the languages they are programmed from. AI assisted tools are sometimes imprecise or do not have adequate vocabulary to interpret or represent languages in different dialects or cultural contexts. So it is best to use LLM tools like this one as a study guide rather than as the sole means of study. If you have access to a live language teacher, please compare and discuss concepts from these lessons with them. LLMs themselves are still learning and will improve with time and further development in the open source ecosystem.",
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            "Note that Android doesn’t have voice synthesis capability for all languages at present. But this may expand over time as more languages are enabled.",
            color = MaterialTheme.colorScheme.onBackground,
        )
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

    AppDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
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

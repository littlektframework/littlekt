package com.littlekt.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.littlekt.createLittleKtApp
import com.littlekt.log.Logger
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

const val HTML_JETPACK_COMPOSE_ELEMENT_ID = "jetpack-compose"
const val HTML_WEBGPU_ELEMENT_ID = "webgpu"

@OptIn(DelicateCoroutinesApi::class, ExperimentalComposeUiApi::class)
fun main() {
    val firstKey = availableExamples.keys.first()
    val chosenKey = window.localStorage.getItem("selectedExampleKey") ?: firstKey
    val exampleInfo = availableExamples[chosenKey] ?: availableExamples[firstKey]!!
    val (title, example) = exampleInfo

    ComposeViewport(document.getElementById(HTML_JETPACK_COMPOSE_ELEMENT_ID)!!) {
        var drawerOpen by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
        }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { drawerOpen = !drawerOpen }
                    .padding(12.dp)
                    .background(Color(0x33000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LittleKt WebGPU Example ${availableExamples.keys.indexOf(chosenKey) + 1}/${availableExamples.keys.size} · $title",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (drawerOpen) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(Color(0xCC1E1542))
                        .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Examples",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    availableExamples.forEach { (key, pair) ->
                        val (itemTitle, _) = pair
                        val selected = key == chosenKey
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (selected) Color(0x3311FFAA) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    // Save and reload the page
                                    window.localStorage.setItem("selectedExampleKey", key)
                                    window.location.reload()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(itemTitle, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    val canvas = document.getElementById(HTML_WEBGPU_ELEMENT_ID) as HTMLCanvasElement
    val startWidth = window.innerWidth
    val startHeight = window.innerHeight

    Logger.setLevels(Logger.Level.DEBUG)
    GlobalScope.launch {
        runCatching {
            createLittleKtApp {
                width = startWidth
                height = startHeight
                this.title = "$title Example"
                canvasId = HTML_WEBGPU_ELEMENT_ID
            }.start(example)
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun syncCanvasSize() {
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
    }
    syncCanvasSize()
    window.onresize = { syncCanvasSize() }
}

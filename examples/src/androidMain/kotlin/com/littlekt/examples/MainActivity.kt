package com.littlekt.examples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.LittleKtSurfaceView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().systemBarsPadding()
                ) {
                    LittleKtSurface({
                        TriangleExample(it)
                    }, Modifier.fillMaxSize())

                    Text("Hello Jetpack Compose", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun LittleKtSurface(
    gameBuilder: (app: Context) -> ContextListener, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = remember {
        LittleKtSurfaceView(context).apply {
            game = gameBuilder
        }
    }
    DisposableEffect(view) {
        onDispose {
            view.release()
        }
    }
    AndroidView(modifier = modifier, factory = { view }, update = { })
}
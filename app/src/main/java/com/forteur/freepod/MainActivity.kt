package com.forteur.freepod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.navigation.FreePodNavGraph
import com.forteur.freepod.ui.theme.FreePodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreePodTheme {
                FreePodApp()
            }
        }
    }
}

@Composable
private fun FreePodApp() {
    var selectedEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }

    FreePodNavGraph(
        selectedEpisode = selectedEpisode,
        onEpisodeSelected = { selectedEpisode = it }
    )
}

@Preview(showBackground = true)
@Composable
private fun FreePodAppPreview() {
    FreePodTheme {
        FreePodApp()
    }
}

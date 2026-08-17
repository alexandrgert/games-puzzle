package ru.alexandrgert.gamespuzzle.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import ru.alexandrgert.gamespuzzle.data.AssetCatalog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assetManager = assets
        setContent {
            MaterialTheme {
                PuzzleNavHost(
                    navController = rememberNavController(),
                    catalog = remember(assetManager) { AssetCatalog.load(assetManager) },
                    assets = assetManager,
                )
            }
        }
    }
}

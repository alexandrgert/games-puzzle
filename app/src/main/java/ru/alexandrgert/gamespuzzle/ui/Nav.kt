package ru.alexandrgert.gamespuzzle.ui

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.domain.CatalogFile
import ru.alexandrgert.gamespuzzle.domain.GridSize
import ru.alexandrgert.gamespuzzle.ui.catalog.CatalogScreen
import ru.alexandrgert.gamespuzzle.ui.play.PlayScreen
import ru.alexandrgert.gamespuzzle.ui.preview.PreviewScreen

object Routes {
    const val CATALOG = "catalog"
    const val PREVIEW = "preview/{id}/{source}"
    const val PLAY = "play/{id}/{source}/{n}"
    const val SETTINGS = "settings"
    const val MY_PHOTOS = "myphotos"
    const val CREDITS = "credits"

    fun preview(id: String, source: String): String =
        "preview/${Uri.encode(id)}/${Uri.encode(source)}"

    fun play(id: String, source: String, n: Int): String =
        "play/${Uri.encode(id)}/${Uri.encode(source)}/$n"
}

@Composable
fun PuzzleNavHost(
    navController: NavHostController,
    catalog: CatalogFile,
    assets: AssetManager,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CATALOG,
    ) {
        composable(Routes.CATALOG) {
            CatalogScreen(
                catalog = catalog,
                assets = assets,
                onPuzzleClick = { id ->
                    navController.navigate(Routes.preview(id, SOURCE_BUILTIN))
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onMyPhotosClick = { navController.navigate(Routes.MY_PHOTOS) },
                onCreditsClick = { navController.navigate(Routes.CREDITS) },
            )
        }
        composable(Routes.PREVIEW) { entry ->
            val id = entry.arguments?.getString(ARG_ID)
            val source = entry.arguments?.getString(ARG_SOURCE)
            val puzzle = catalog.puzzles.firstOrNull {
                source == SOURCE_BUILTIN && it.id == id
            }
            PreviewScreen(
                puzzle = puzzle,
                assets = assets,
                onStart = { gridSize ->
                    if (id != null && source != null) {
                        navController.navigate(Routes.play(id, source, gridSize.n))
                    }
                },
            )
        }
        composable(Routes.PLAY) { entry ->
            val id = entry.arguments?.getString(ARG_ID)
            val source = entry.arguments?.getString(ARG_SOURCE)
            val size = entry.arguments?.getString(ARG_SIZE)?.toIntOrNull()?.let { n ->
                GridSize.entries.firstOrNull { it.n == n }
            }
            val puzzle = catalog.puzzles.firstOrNull {
                source == SOURCE_BUILTIN && it.id == id
            }
            val bitmap = remember(puzzle?.file, assets) {
                puzzle?.let {
                    runCatching {
                        assets.open(it.file).use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
            }
            if (size == null) {
                PlaceholderScreen(R.string.play_invalid_size)
            } else {
                PlayScreen(
                    sourceBitmap = bitmap,
                    size = size,
                    statsEnabled = false,
                    onAbandon = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(R.string.screen_settings)
        }
        composable(Routes.MY_PHOTOS) {
            PlaceholderScreen(R.string.screen_my_photos)
        }
        composable(Routes.CREDITS) {
            PlaceholderScreen(R.string.screen_credits)
        }
    }
}

@Composable
private fun PlaceholderScreen(@StringRes textResource: Int) {
    Text(
        text = stringResource(textResource),
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(),
    )
}

private const val SOURCE_BUILTIN = "builtin"
private const val ARG_ID = "id"
private const val ARG_SOURCE = "source"
private const val ARG_SIZE = "n"

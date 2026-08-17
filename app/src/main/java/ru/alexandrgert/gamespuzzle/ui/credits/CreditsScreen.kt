package ru.alexandrgert.gamespuzzle.ui.credits

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.domain.CatalogPuzzle

@Composable
fun CreditsScreen(
    puzzles: List<CatalogPuzzle>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(puzzles, key = { it.id }) { puzzle ->
            CreditEntry(puzzle = puzzle)
        }
    }
}

@Composable
private fun CreditEntry(puzzle: CatalogPuzzle) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                R.string.credits_attribution,
                puzzle.titleRu,
                puzzle.attribution,
                puzzle.license,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = puzzle.sourceUrl,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(puzzle.sourceUrl)),
                )
            },
        )
    }
}

fun formatCreditsAttribution(
    titleRu: String,
    attribution: String,
    license: String,
): String = "$titleRu — $attribution — $license"

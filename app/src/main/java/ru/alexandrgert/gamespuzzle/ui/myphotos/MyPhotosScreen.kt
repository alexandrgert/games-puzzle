package ru.alexandrgert.gamespuzzle.ui.myphotos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.alexandrgert.gamespuzzle.R
import ru.alexandrgert.gamespuzzle.data.RecordsStore
import ru.alexandrgert.gamespuzzle.data.UserPuzzle
import ru.alexandrgert.gamespuzzle.data.UserPuzzlesStore
import ru.alexandrgert.gamespuzzle.platform.UserFiles
import ru.alexandrgert.gamespuzzle.platform.UserPhotoImportResult

@Composable
fun MyPhotosScreen(
    userFiles: UserFiles,
    userPuzzlesStore: UserPuzzlesStore,
    recordsStore: RecordsStore,
    onPuzzleClick: (String) -> Unit,
) {
    var puzzles by remember { mutableStateOf<List<UserPuzzle>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val imageOpenError = stringResource(R.string.error_image_open)
    val imageTooSmallError = stringResource(R.string.error_image_too_small)
    val importSuccess = stringResource(R.string.photo_imported)
    val deleteSuccess = stringResource(R.string.photo_deleted)
    val deleteError = stringResource(R.string.error_photo_delete)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            when (val result = userFiles.importPhoto(uri)) {
                is UserPhotoImportResult.Success -> {
                    val updated = runCatching {
                        userPuzzlesStore.add(result.puzzle)
                    }.getOrElse {
                        userFiles.delete(result.puzzle)
                        snackbarHostState.showSnackbar(imageOpenError)
                        return@launch
                    }
                    puzzles = updated
                    snackbarHostState.showSnackbar(importSuccess)
                }

                UserPhotoImportResult.TooSmall ->
                    snackbarHostState.showSnackbar(imageTooSmallError)

                UserPhotoImportResult.OpenError ->
                    snackbarHostState.showSnackbar(imageOpenError)
            }
        }
    }

    LaunchedEffect(userPuzzlesStore) {
        puzzles = userPuzzlesStore.load()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Button(
                onClick = { importLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(stringResource(R.string.action_add_photo))
            }
            if (puzzles.isEmpty()) {
                Text(
                    text = stringResource(R.string.my_photos_empty),
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(puzzles, key = { it.id }) { puzzle ->
                        UserPhotoItem(
                            puzzle = puzzle,
                            userFiles = userFiles,
                            onClick = { onPuzzleClick(puzzle.id) },
                            onDelete = {
                                coroutineScope.launch {
                                    val deleted = runCatching {
                                        check(userFiles.delete(puzzle))
                                        val updated = userPuzzlesStore.remove(puzzle.id)
                                        recordsStore.delete(puzzle.id)
                                        updated
                                    }
                                    deleted.onSuccess {
                                        puzzles = it
                                        snackbarHostState.showSnackbar(deleteSuccess)
                                    }.onFailure {
                                        snackbarHostState.showSnackbar(deleteError)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPhotoItem(
    puzzle: UserPuzzle,
    userFiles: UserFiles,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val bitmap = remember(puzzle.id, userFiles) { userFiles.load(puzzle.id) }
    Column {
        if (bitmap != null) {
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.puzzle_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(160.dp),
                )
            }
        }
        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_delete))
        }
    }
}

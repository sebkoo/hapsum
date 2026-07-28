package io.github.sebkoo.hapsum.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onIntent(CaptureUiIntent.PermissionResult(granted))
        }

    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        viewModel.onIntent(CaptureUiIntent.PermissionResult(granted))
    }

    LaunchedEffect(state.hasCameraPermission, lifecycleOwner) {
        if (state.hasCameraPermission) {
            viewModel.bindCamera(lifecycleOwner)
        }
    }

    CaptureContent(
        state = state,
        onIntent = viewModel::onIntent,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        modifier = modifier,
    )
}

@Composable
internal fun CaptureContent(
    state: CaptureUiState,
    onIntent: (CaptureUiIntent) -> Unit,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            !state.hasCameraPermission -> {
                PermissionRationale(onRequestPermission = onRequestPermission)
            }

            state.surfaceRequest == null -> {
                CircularProgressIndicator()
            }

            else -> {
                CameraPreview(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.capture_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.capture_permission_request))
        }
    }
}

@Composable
private fun CameraPreview(
    state: CaptureUiState,
    onIntent: (CaptureUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceRequest = state.surfaceRequest ?: return
    Column(modifier = modifier.fillMaxSize()) {
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.error != null) {
                Text(
                    text = stringResource(R.string.capture_save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = { onIntent(CaptureUiIntent.CapturePhotoClicked) },
                enabled = !state.isSaving,
            ) {
                Text(text = stringResource(R.string.capture_button))
            }
        }
    }
}

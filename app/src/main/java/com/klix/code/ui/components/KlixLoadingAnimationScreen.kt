package com.klix.code.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.klix.code.R
import kotlinx.coroutines.delay

private val SplashBackgroundColor = Color(0xFF0E1118)

/**
 * KlixLoadingAnimationScreen plays the custom Klix animated loading sequence
 * when the app opens, seamlessly fitting all screen ratios with a sleek
 * dark background and instant tap-to-skip option.
 */
@Composable
fun KlixLoadingAnimationScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFinished by remember { mutableStateOf(false) }

    val safeFinish = remember(onAnimationFinished) {
        {
            if (!isFinished) {
                isFinished = true
                onAnimationFinished()
            }
        }
    }

    // Safety timeout: In case device decoder fails or onCompletion is delayed,
    // ensure the IDE loads within 4.5 seconds.
    LaunchedEffect(Unit) {
        delay(4500)
        safeFinish()
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                safeFinish()
            },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val videoRatio = 9f / 16f
            val screenRatio = maxWidth.value / maxHeight.value

            val videoModifier = if (screenRatio > videoRatio) {
                // Screen is wider than video (tablet or landscape)
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(videoRatio)
            } else {
                // Screen is taller than video (typical smartphone)
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoRatio)
            }

            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        isOpaque = true
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                try {
                                    val s = Surface(surface)
                                    val player = MediaPlayer().apply {
                                        val afd = ctx.resources.openRawResourceFd(R.raw.klix_loading_animation)
                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                        setSurface(s)
                                        isLooping = false
                                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                        setOnPreparedListener { mp ->
                                            mp.start()
                                        }
                                        setOnCompletionListener {
                                            safeFinish()
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            safeFinish()
                                            true
                                        }
                                        prepareAsync()
                                    }
                                    mediaPlayer = player
                                } catch (e: Exception) {
                                    safeFinish()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                } catch (_: Exception) {}
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = videoModifier
            )
        }

        // Tap-to-skip chip in the top corner for instant IDE access
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { safeFinish() },
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33FFFFFF),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Loading Animation",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

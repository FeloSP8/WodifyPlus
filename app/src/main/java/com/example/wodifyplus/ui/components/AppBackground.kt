package com.example.wodifyplus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.wodifyplus.R

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    showPattern: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        if (showPattern) {
            // Android automáticamente selecciona drawable o drawable-night según el tema
            Image(
                painter = painterResource(R.drawable.app_background),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isDarkTheme) 0.4f else 0.3f), // Ajuste de transparencia por tema
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBackgroundPreview() {
    AppBackground {
        // Preview content
    }
}

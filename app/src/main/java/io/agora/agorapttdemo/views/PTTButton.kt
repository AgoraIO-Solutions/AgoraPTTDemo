package io.agora.agorapttdemo.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.agora.agorapttdemo.ui.theme.AgoraPTTDemoTheme
import kotlin.math.min

@Composable
fun PTTButton() {
    val config = LocalConfiguration.current
    val minDim = min(config.screenWidthDp, config.screenHeightDp)
        Surface(
            modifier = Modifier
                .size(minDim.dp - 20.dp)
                .clip(CircleShape),
            color = Color.Red
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PTT", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }

        }
}

@Preview("Unpressed PTT Button", showBackground = true)
@Composable
fun PTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton()
        }
    }
}
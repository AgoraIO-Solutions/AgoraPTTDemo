package io.agora.agorapttdemo.views

import android.content.res.Resources
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.agorapttdemo.ui.theme.AgoraPTTDemoTheme
import io.agora.agorapttdemo.viewmodels.PTTButtonViewModel
import io.agora.agorapttdemo.viewmodels.PTTState
import kotlin.math.min

@Composable
fun PTTButton(pttButtonViewModel: PTTButtonViewModel = viewModel()) {
    val config = LocalConfiguration.current
    val minDim = min(config.screenWidthDp, config.screenHeightDp)
    val buttonState = pttButtonViewModel.state.observeAsState(initial = PTTState.INACTIVE)

    val infiniteTransition = rememberInfiniteTransition()
    val color: Color = when(buttonState.value) {
        PTTState.INACTIVE -> Color.Gray
        PTTState.BROADCASTING -> MaterialTheme.colors.primary
        PTTState.RECEIVING -> MaterialTheme.colors.secondary
        PTTState.CONNECTING -> infiniteTransition.animateColor(
            initialValue = MaterialTheme.colors.primary,
            targetValue = MaterialTheme.colors.secondary,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    }

        Surface(
            modifier = Modifier
                .size(minDim.dp - 20.dp)
                .clip(CircleShape),
            color = color
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
            PTTButton(PTTButtonViewModel(PTTState.INACTIVE))
        }
    }
}

@Preview("Connecting PTT Button", showBackground = true)
@Composable
fun ConnectingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.CONNECTING))
        }
    }
}

@Preview("Broadcasting PTT Button", showBackground = true)
@Composable
fun BroadcastingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.BROADCASTING))
        }
    }
}

@Preview("Receiving PTT Button", showBackground = true)
@Composable
fun ReceivingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.RECEIVING))
        }
    }
}
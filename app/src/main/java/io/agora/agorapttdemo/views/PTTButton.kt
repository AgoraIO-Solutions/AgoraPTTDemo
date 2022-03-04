package io.agora.agorapttdemo.views

import android.view.MotionEvent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.agorapttdemo.viewmodels.PTTButtonViewModel
import io.agora.agorapttdemo.viewmodels.PTTState
import kotlin.math.min

@ExperimentalComposeUiApi
@Composable
fun PTTButton(pttButtonViewModel: PTTButtonViewModel = viewModel()) {
    val buttonState: PTTState by pttButtonViewModel.state.observeAsState(initial = PTTState.INACTIVE)
    val infiniteTransition = rememberInfiniteTransition()
    val connectingColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colors.primary,
        targetValue = MaterialTheme.colors.secondary,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color: Color = when(buttonState) {
        PTTState.INACTIVE -> Color.Gray
        PTTState.BROADCASTING -> MaterialTheme.colors.primary
        PTTState.RECEIVING -> MaterialTheme.colors.secondary
        PTTState.CONNECTING -> connectingColor
    }

    val config = LocalConfiguration.current
    val minDim = min(config.screenWidthDp, config.screenHeightDp)

    Surface(
        modifier = Modifier
            .size(minDim.dp - 20.dp)
            .clip(CircleShape)
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> pttButtonViewModel.pttPushed()
                    MotionEvent.ACTION_MOVE -> {
                       /* noop */
                    }
                    MotionEvent.ACTION_UP -> pttButtonViewModel.pttStop()
                    else -> false
                }
                true
            },
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

/* TODO: Fix previews, when we do design. This will require activities that handle HilT

@ExperimentalComposeUiApi
@Preview("Unpressed PTT Button", showBackground = true)
@Composable
fun PTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.INACTIVE))
        }
    }
}

@ExperimentalComposeUiApi
@Preview("Connecting PTT Button", showBackground = true)
@Composable
fun ConnectingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.CONNECTING))
        }
    }
}

@ExperimentalComposeUiApi
@Preview("Broadcasting PTT Button", showBackground = true)
@Composable
fun BroadcastingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.BROADCASTING))
        }
    }
}

@ExperimentalComposeUiApi
@Preview("Receiving PTT Button", showBackground = true)
@Composable
fun ReceivingPTTButtonPreview() {
    AgoraPTTDemoTheme {
        Surface(modifier = Modifier.size(200.dp)) {
            PTTButton(PTTButtonViewModel(PTTState.RECEIVING))
        }
    }
}



 */
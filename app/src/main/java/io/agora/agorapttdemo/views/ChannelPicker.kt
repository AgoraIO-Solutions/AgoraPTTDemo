package io.agora.agorapttdemo.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.agorapttdemo.ui.theme.AgoraPTTDemoTheme
import io.agora.agorapttdemo.viewmodels.ChannelConfigurationViewModel
import io.agora.agorapttdemo.viewmodels.ChannelConfigurationViewModel.ChannelType

@Composable
fun ChannelPicker(viewModel: ChannelConfigurationViewModel = viewModel()) {
    val channelType = viewModel.channelType.observeAsState()
    val coldBackground = if (channelType.value == ChannelType.COLD) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
    val hotBackground = if (channelType.value == ChannelType.HOT) MaterialTheme.colors.primary else MaterialTheme.colors.secondary

    Surface() {
        Row(modifier = Modifier.padding(5.dp)) {
            ChannelButton(text = "Hot", color = hotBackground ) {
                viewModel.channelType.value = ChannelType.HOT
            }
            Spacer(modifier = Modifier.defaultMinSize(20.dp) )
            ChannelButton(text = "Cold", color = coldBackground ) {
                viewModel.channelType.value = ChannelType.COLD
            }
        }
    }
}

@Composable
private fun ChannelButton(text: String, color: Color, onClick: () -> Unit) {
    val config = LocalConfiguration.current
    val width = config.screenWidthDp * 0.4

    Button(
        modifier = Modifier.defaultMinSize(minWidth = width.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        onClick = onClick) {
        Text(text)
    }
}


@Preview(name = "Channel Picker")
@Composable
fun ChannelPickerPreview() {
    AgoraPTTDemoTheme {
        ChannelPicker()
    }
}
package io.agora.agorapttdemo.views

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.agorapttdemo.ui.theme.AgoraPTTDemoTheme

@Composable
fun PTTScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChannelPicker()
        Spacer(modifier = Modifier.defaultMinSize(minHeight = 20.dp))
        AlerterSelector()
        Spacer(modifier = Modifier.defaultMinSize(minHeight = 100.dp))
        PTTButton()
    }
}

@Preview(name = "PTT Screen Preview")
@Composable
fun PTTScreenPreview() {
    AgoraPTTDemoTheme {
        PTTScreen()
    }
}
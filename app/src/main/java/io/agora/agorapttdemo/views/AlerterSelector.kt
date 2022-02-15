package io.agora.agorapttdemo.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.agorapttdemo.ui.theme.AgoraPTTDemoTheme
import io.agora.agorapttdemo.viewmodels.PTTButtonViewModel

@Composable
fun AlerterSelector(viewModel: PTTButtonViewModel = viewModel()) {
    val alertsOn = viewModel.alertsOn.observeAsState(true)
    Surface {
        Row {
            Text(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                text ="Alerts On?"
            )
            Spacer(modifier = Modifier.defaultMinSize(minWidth = 10.dp))
            Checkbox(checked = alertsOn.value, onCheckedChange = { viewModel.alertsOn.value = it }, modifier = Modifier.size(30.dp))
        }
    }
}

@Preview(name = "Alerter Selection")
@Composable
fun AlerterSelectorPreview() {
    AgoraPTTDemoTheme {
        AlerterSelector()
    }
}
package com.netprobe.diagnostics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprobe.diagnostics.data.oui.OuiLookup
import com.netprobe.diagnostics.ui.theme.TerminalAmber
import com.netprobe.diagnostics.ui.theme.TextDisabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VendorLabel(
    mac: String?,
    modifier: Modifier = Modifier
) {
    if (mac.isNullOrBlank()) return
    val context = LocalContext.current
    var vendor by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mac) {
        withContext(Dispatchers.IO) {
            OuiLookup.init(context)
            vendor = OuiLookup.getVendorName(mac)
        }
    }

    AnimatedVisibility(
        visible = vendor != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = vendor ?: "",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TerminalAmber,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

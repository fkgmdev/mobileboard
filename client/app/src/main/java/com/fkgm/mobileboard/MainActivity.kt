package com.fkgm.mobileboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.fkgm.mobileboard.ui.theme.MobileboardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileboardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KeyboardInput(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private data class PendingSend(val message: String, val ip: String, val port: String)

private fun hasLocalNetworkAccess(context: Context): Boolean {
    return Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_LOCAL_NETWORK
        ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun KeyboardInput(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var pendingSend by remember { mutableStateOf<PendingSend?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun sendMessage(message: String, ipAddress: String, portNumber: String) {
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sendUdpPacket(message, ipAddress.trim(), portNumber.toInt())
                }
                status = "sent: $message"
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            }
        }
    }

    val requestLocalNetworkPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingSend
        pendingSend = null
        if (granted && request != null) {
            sendMessage(request.message, request.ip, request.port)
        } else if (!granted) {
            status = "Local network permission is required to send UDP packets"
        }
    }

    Column(modifier = modifier) {
        Row(modifier = modifier) {
            OutlinedTextField(
                value = ip,
                onValueChange = { newText ->
                    ip = newText
                },
                label = { Text("Ip address") }
            )
            OutlinedTextField(
                value = port,
                onValueChange = { newText ->
                    port = newText
                },
                label = { Text("Port") }
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
            },
            label = { Text("Input") }
        )
        Button(
            onClick = {
                val msgToSend = text
                text = ""

                if (hasLocalNetworkAccess(context)) {
                    sendMessage(msgToSend, ip, port)
                } else {
                    pendingSend = PendingSend(msgToSend, ip, port)
                    requestLocalNetworkPermission.launch(
                        Manifest.permission.ACCESS_LOCAL_NETWORK
                    )
                }
            },
            enabled = text.isNotBlank()
        ) {
            Text("Submit")
        }
        if (status.isNotEmpty()) {
            Text(status)
        }
    }
}

fun sendUdpPacket(message: String, ipAddress: String, port: Int) {
    val data = message.toByteArray(Charsets.UTF_8)
    val address = InetAddress.getByName(ipAddress)
    val packet = DatagramPacket(data, data.size, address, port)

    DatagramSocket().use { socket ->
        socket.broadcast = true
        socket.send(packet)
    }
}

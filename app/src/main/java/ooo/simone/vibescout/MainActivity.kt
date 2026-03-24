package ooo.simone.vibescout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import kotlinx.coroutines.launch
import ooo.simone.vibescout.core.preferences.SharedPreferencesManager
import ooo.simone.vibescout.core.workers.getWorkerStatus
import ooo.simone.vibescout.core.workers.startWorker
import ooo.simone.vibescout.core.workers.stopWorker
import ooo.simone.vibescout.ui.theme.VibeScoutTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLogging()
        lifecycleScope.launch {
            initPermissions()
        }

        enableEdgeToEdge()
        setContent {
            VibeScoutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Container(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun initPermissions() {
        XXPermissions.with(this)
            // Request multiple permission
            .permission(PermissionLists.getPostNotificationsPermission())
            .permission(PermissionLists.getRecordAudioPermission())
            .request { p0, p1 ->  }
    }

    private fun initLogging() {
        Timber.plant(Timber.DebugTree())
    }
}

@Composable
fun Container(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var textValue by remember { mutableStateOf(SharedPreferencesManager.getAuthKeySP(context) ?: "") }
    
    // Observe worker status
    val workerInfo by getWorkerStatus(context).observeAsState()
    
    // Logic to determine button states based on worker status
    val isWorkerRunning = workerInfo?.state == WorkInfo.State.RUNNING || 
                         workerInfo?.state == WorkInfo.State.ENQUEUED
    
    val isStartEnabled = !isWorkerRunning
    val isStopEnabled = isWorkerRunning

    // Method to set the string to a variable
    fun setStringValue(value: String) {
       SharedPreferencesManager.setAuthKeySP(context, value)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text("Auth Key") },
                placeholder = { Text("Enter your key here...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { setStringValue(textValue) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(64.dp)
                    .padding(top = 8.dp)
            ) {
                Icon(ImageVector.vectorResource(R.drawable.save_24px), contentDescription = "Set")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { startWorker(context) },
            enabled = isStartEnabled,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Avvia Worker")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { stopWorker(context) },
            enabled = isStopEnabled,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Ferma Worker")
        }
        
        // Optional: Show status text
        workerInfo?.let {
            Text(
                text = "Status: ${it.state}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

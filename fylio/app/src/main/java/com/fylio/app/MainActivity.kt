package com.fylio.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fylio.app.core.pdf.PdfEngine
import com.fylio.app.ui.FylioApp
import com.fylio.app.ui.FylioViewModel
import com.fylio.app.ui.theme.FylioTheme

class MainActivity : ComponentActivity() {

    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfEngine.init(applicationContext)
        enableEdgeToEdge()
        incomingUri = intent?.incomingStream()

        setContent {
            FylioTheme {
                val vm: FylioViewModel = viewModel()
                FylioApp(
                    viewModel = vm,
                    incomingUri = incomingUri,
                    onIncomingHandled = { incomingUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = intent.incomingStream()
    }

    /** "Open with Fylio" support: copy the incoming stream into the documents folder. */
    private fun Intent.incomingStream(): Uri? {
        val uri = data ?: return null
        if (uri.scheme == "content" || uri.scheme == "file") return uri
        return null
    }
}

package pl.bkacala.fiszkide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.bkacala.fiszkide.ui.FiszkiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val databasePath = applicationContext.getDatabasePath("fiszki.db").also {
            check(it.parentFile!!.isDirectory || it.parentFile!!.mkdirs())
        }.absolutePath
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(databasePath) as T
        })[StudyViewModel::class.java]
        setContent {
            val state by model.controller.state.collectAsState()
            BackHandler(enabled = state.session != null) { model.controller.endSession() }
            FiszkiApp(model.controller)
        }
    }
}

class StudyViewModel(databasePath: String) : ViewModel() {
    val controller = createStudyController(databasePath)
    override fun onCleared() { controller.close() }
}

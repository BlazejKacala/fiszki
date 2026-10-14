package pl.bkacala.fiszkide

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import pl.bkacala.fiszkide.ui.FiszkiApp

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    val controller = remember {
        val directory = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory, NSUserDomainMask, true,
        ).first() as String
        NSFileManager.defaultManager.createDirectoryAtPath(
            directory, withIntermediateDirectories = true, attributes = null, error = null,
        )
        createStudyController("$directory/fiszki.db")
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    FiszkiApp(controller)
}

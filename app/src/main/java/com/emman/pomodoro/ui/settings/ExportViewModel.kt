package com.emman.pomodoro.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.emman.pomodoro.data.export.DataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exporter: DataExporter
) : ViewModel() {

    suspend fun exportTo(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = exporter.exportJson()
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Cannot open output stream")
        }.isSuccess
    }

    suspend fun importFrom(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("Cannot open input stream")
            exporter.importJson(raw)
        }.isSuccess
    }
}

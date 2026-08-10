package com.col.notnetmediaforge.data.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Date

/**
 * Guarda en un archivo la traza del último crash de la app para poder
 * diagnosticarlo: `adb shell run-as com.col.notnetmediaforge cat files/crash.log`
 */
object CrashLogger {

    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = File(context.filesDir, "crash.log")
                val trace = Log.getStackTraceString(throwable)
                file.appendText("\n==== ${Date()} [${thread.name}] ====\n$trace\n")
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

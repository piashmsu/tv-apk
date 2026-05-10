package com.piashmsu.tvapk

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val TAG = "TvApk_Debug"
    private const val LOG_FILE = "tvapk_crash.log"
    private const val MAX_LOG_SIZE = 512 * 1024

    private var logFile: File? = null
    private var app: TvApkApp? = null

    fun init(app: TvApkApp) {
        this.app = app
        logFile = File(app.cacheDir, LOG_FILE)
        if ((logFile?.length() ?: 0) > MAX_LOG_SIZE) {
            logFile?.delete()
        }
    }

    fun log(message: String) {
        val stamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$stamp] $message\n"
        Log.d(TAG, message)
        runCatching {
            logFile?.appendText(line)
        }
    }

    fun logError(tag: String, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$stamp] ERROR $tag\n${sw}\n"
        Log.e(TAG, "ERROR $tag", throwable)
        runCatching {
            logFile?.appendText(line)
        }
    }

    fun logError(tag: String, message: String) {
        val stamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$stamp] ERROR $tag: $message\n"
        Log.e(TAG, "ERROR $tag: $message")
        runCatching {
            logFile?.appendText(line)
        }
    }

    fun getLogText(): String {
        return runCatching {
            logFile?.readText() ?: "No log file found"
        }.getOrDefault("Error reading log file")
    }

    fun clear() {
        runCatching { logFile?.delete() }
    }
}

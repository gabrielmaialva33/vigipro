package com.vigipro.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class VigiProApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        // Crash handler — salva stack trace em arquivo pra debug
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable)
            } catch (_: Exception) {
                // Nao pode crashar no crash handler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        val crashDir = File(filesDir, "crash_logs")
        crashDir.mkdirs()

        // Manter apenas os ultimos 10 crash logs
        crashDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(9)
            ?.forEach { it.delete() }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val crashFile = File(crashDir, "crash_$timestamp.txt")

        PrintWriter(FileWriter(crashFile)).use { writer ->
            writer.println("VigiPro Crash Report")
            writer.println("Time: $timestamp")
            writer.println("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            writer.println("---")
            throwable.printStackTrace(writer)
        }

        Timber.e(throwable, "App crashed")
    }

    /** Tree pra release: loga apenas WARN+ e suprime verbose/debug */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < Log.WARN) return
            Log.println(priority, tag ?: "VigiPro", message)
        }
    }
}

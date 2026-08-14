package org.whatever.logger

import kotlin.time.Clock
import java.io.BufferedWriter
import java.io.File

class Logger(
    logDirectory: File,
    logFilePath: String,
) {
    private val logWriter: BufferedWriter
    private val logFile = File("${logDirectory.path}/$logFilePath")

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdir()
        }
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        logWriter = logFile.bufferedWriter()
        info("Logger initialized")
    }

    fun info(string: String) {
        println("[info]: $string")
        logWriter.write("${Clock.System.now().epochSeconds} [info]: $string\n")
        logWriter.flush()
    }

    fun warning(string: String) {
        println("\u001B[33m[warning]: $string\u001B[0m")
        logWriter.write("${Clock.System.now().epochSeconds} [warning]: $string\n")
        logWriter.flush()
    }

    fun error(string: String) {
        println("\u001B[31m[error]: $string\u001B[0m")
        logWriter.write("${Clock.System.now().epochSeconds} [error]: $string\n")
        logWriter.flush()
    }

    fun exception(e: Exception) {
        var error = e.toString()
        println(e.toString())
        for (i in e.stackTrace) {
            println(i.toString())
            error += "\n$i"
        }
        error(error)
    }
}

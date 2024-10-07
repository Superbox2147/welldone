package org.whatever.logger

import com.sun.jna.Function.*
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import kotlinx.datetime.Clock
import java.io.BufferedWriter
import java.io.File

class Logger(logDirectory: File, logFilePath: String) {
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
        enableConsoleColors()
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

    private fun enableConsoleColors() {
        if (System.getProperty("os.name").lowercase().startsWith("windows")) {
            info("Enabling color escape codes for console")
            val stdHandleFunction = getFunction("kernel32","GetStdHandle")
            val stdOutputHandle = WinDef.DWORD(-11)
            val handleOut = stdHandleFunction.invoke(WinNT.HANDLE::class.java, arrayOf(stdOutputHandle)) as WinNT.HANDLE

            val preDwmMode = WinDef.DWORDByReference(WinDef.DWORD(0))
            val getConsoleModeFunction = getFunction("kernel32", "GetConsoleMode")
            getConsoleModeFunction.invoke(WinDef.BOOL::class.java, arrayOf(handleOut, preDwmMode))

            val enableVirtualTerminalProcessing = 4
            val dwmMode = preDwmMode.value
            dwmMode.setValue((dwmMode.toInt() or enableVirtualTerminalProcessing).toLong())
            val setConsoleModeFunction = getFunction("kernel32", "SetConsoleMode")
            setConsoleModeFunction.invoke(WinDef.BOOL::class.java, arrayOf(handleOut, dwmMode))
        } else {
            info("Skipping color enable step, system is not Windows")
        }
    }
}
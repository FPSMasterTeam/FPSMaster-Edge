package top.fpsmaster.utils.os

import top.fpsmaster.FPSMaster
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.modules.logger.Logger.error
import top.fpsmaster.modules.logger.Logger.info
import top.fpsmaster.wrapper.Constants
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files

object FileUtils {
    private var fpsmasterCache: File
    private var netease: File

    var dir: File
    var plugins: File
    var cache: File
    var music: File
    var artists: File
    var omaments: File
    var background: File
    var hasBackground = false

    init {
        cache = file(ProviderManager.mcProvider.getGameDir(), ".cache")
        dir = file(ProviderManager.mcProvider.getGameDir(), "FPSMaster " + Constants.VERSION)
        plugins = file(dir, "plugins")
        fpsmasterCache = file(cache, "FPSMasterClient")
        netease = file(cache, "netease")
        music = file(netease, "songs")
        artists = file(netease, "artists")
        omaments = file(cache, "omaments")
        background = File(dir,"background.png")
        if (background.exists())
            hasBackground = true
    }

    fun file(parent: File, child: String): File {
        val file = File(parent, child)
        if (!file.exists())
            file.mkdirs()
        return file
    }


    @JvmStatic
    fun saveFileBytes(s: String, bytes: ByteArray) {
        val file = File(dir, s)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val fOut = FileOutputStream(file)
            fOut.write(bytes)
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun saveFile(name: String, content: String) {
        val file = File(dir, name)
        saveAbsoluteFile(file.absolutePath, content)
    }

    private fun saveAbsoluteFile(name: String, content: String) {
        val file = File(name)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    error("FileUtils", "failed to create $name")
                }
            }
            val bw = BufferedWriter(OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))
            bw.write(content)
            bw.flush()
            bw.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveTempValue(name: String, value: String) {
        val dir = File(fpsmasterCache, "$name.tmp")
        saveAbsoluteFile(dir.absolutePath, value)
    }

    fun readTempValue(name: String): String {
        try {
            val dir = File(fpsmasterCache, "$name.tmp")
            return if (!dir.exists()) {
                ""
            } else readAbsoluteFile(dir.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    @JvmStatic
    fun release(file: String) {
        val f = File(dir, "file.lang").absoluteFile
        info("release $file")
        try {
            f.createNewFile()
            val resourceAsStream = FileUtils::class.java.getResourceAsStream("/assets/minecraft/client/lang/$file.lang")
            if (resourceAsStream == null) {
                error("An error occurred while loading language file: $file.lang")
                return
            }
            val `in` = InputStreamReader(resourceAsStream, StandardCharsets.UTF_8)
            val reader = BufferedReader(`in`)
            val sb = StringBuilder()
            var s1: String
            while (reader.readLine().also { s1 = it } != null) {
                sb.append(s1 + "\n")
            }
            saveFile("$file.lang", sb.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getDirSize(folder: File): Int {
        var size: Long = 0
        val fileList = folder.listFiles() ?: return 0
        for (file in fileList) {
            size += if (file.isDirectory) {
                getDirSize(file).toLong()
            } else {
                file.length()
            }
        }
        return (size / 1024 / 1024).toInt()
    }

    @JvmStatic
    fun readFile(name: String): String {
        val file = File(dir, name)
        return readAbsoluteFile(file.absolutePath)
    }

    private fun readAbsoluteFile(name: String): String {
        val file = File(name)
        val result = StringBuilder()
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            val fIn = FileInputStream(file)
            BufferedReader(InputStreamReader(fIn, StandardCharsets.UTF_8)).use { bufferedReader ->
                var str: String?
                while (bufferedReader.readLine().also { str = it } != null) {
                    result.append(str)
                    result.append(System.lineSeparator())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.toString()
    }

    @JvmStatic
    fun fixName(s: String): String {
        return s.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }
}

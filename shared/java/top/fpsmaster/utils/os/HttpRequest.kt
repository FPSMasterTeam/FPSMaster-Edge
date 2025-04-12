package top.fpsmaster.utils.os

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import top.fpsmaster.modules.logger.Logger
import top.fpsmaster.modules.logger.Logger.info
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object HttpRequest {
    @JvmStatic
    operator fun get(u: String?): String {
        return getWithCookie(u, "")
    }

    @JvmStatic
    fun getWithCookie(url: String?, cookie: String): String {
        val u = url
        val url = URL(u)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        )
        val value = cookie.replace("\n","")
        if (value.isNotBlank()) {
            connection.setRequestProperty("Cookie", value)
        }
        connection.connectTimeout = 15000
        connection.readTimeout = 5000
        connection.connect()
        val reader = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
        val builder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            builder.append(line)
        }
        reader.close()
        connection.disconnect()
        return builder.toString()
    }

    @JvmStatic
    fun downloadFile(url: String, filepath: String) {
        try {
            val client: HttpClient = HttpClients.createDefault()
            val httpget = HttpGet(url)
            val response = client.execute(httpget)
            val entity = response.entity
            val `is` = entity.content
            var progress: Long = 0
            val totalLen = entity.contentLength
            val unit = totalLen / 100
            val file = File(filepath)
            val fileout = FileOutputStream(file)
            val buffer = ByteArray(10 * 1024)
            var ch: Int
            while (`is`.read(buffer).also { ch = it } != -1) {
                fileout.write(buffer, 0, ch)
                progress += ch.toLong()
            }
            if (progress % 10 == 0L) info("Downloaded " + progress / unit + "%")
            `is`.close()
            fileout.flush()
            fileout.close()
        } catch (e: Exception) {
            Logger.error("Failed to download file: $url")
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun sendPostRequest(targetUrl: String?, body: String, headers: MutableMap<String, String>): Array<String?> {
        val response = arrayOfNulls<String>(2)
        val url = URL(targetUrl)
        val connection = url.openConnection() as HttpURLConnection

        // 设置请求方式为POST
        connection.requestMethod = "POST"

        // 添加headers
        for ((key, value) in headers) {
            connection.setRequestProperty(key, value.trim())
        }

        // 添加body
        connection.doOutput = true
        val os = connection.outputStream
        os.write(body.toByteArray())
        os.flush()
        os.close()

        // 获取响应状态码
        response[0] = connection.responseCode.toString()
        val content = StringBuffer()

        if (response[0] == "400" || response[0] == "403") {
            // 获取响应内容
            val `in` = connection.errorStream.bufferedReader()
            var inputLine: String?
            while (`in`.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            `in`.close()
        }else{
            // 获取响应内容
            val `in` = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8))
            var inputLine: String?
            while (`in`.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            `in`.close()
        }
        connection.disconnect()
        response[1] = content.toString()
        return response
}

fun downloadAsync(url: String?, filepath: String, callback: Runnable) {
        Thread {
            try {
                val client: HttpClient = HttpClients.createDefault()
                val httpget = HttpGet(url)
                val response = client.execute(httpget)
                val entity = response.entity
                val `is` = entity.content
                var progress: Long = 0
                val totalLen = entity.contentLength
                val unit = totalLen / 100
                val file = File(filepath)
                val fileout = FileOutputStream(file)
                val buffer = ByteArray(10 * 1024)
                var ch = 0
                while (`is`.read(buffer).also { ch = it } != -1) {
                    fileout.write(buffer, 0, ch)
                    progress += ch.toLong()
                }
                if (progress % 10 == 0L) info("Downloaded " + progress / unit + "%")
                `is`.close()
                fileout.flush()
                fileout.close()
                callback.run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}

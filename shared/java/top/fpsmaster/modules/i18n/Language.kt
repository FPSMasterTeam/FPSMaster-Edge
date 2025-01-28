package top.fpsmaster.modules.i18n

import top.fpsmaster.utils.os.FileUtils.readFile
import top.fpsmaster.utils.os.FileUtils.release
import top.fpsmaster.utils.os.FileUtils.saveFile

class Language {
    private var prompts: MutableMap<String, String> = HashMap()

    init {
        release("en_us")
        release("zh_cn")
    }

    fun save(language: String) {
        val sb = StringBuilder()
        for ((key, value) in prompts) {
            sb.append(key).append("=").append(value).append(System.lineSeparator())
        }
        saveFile("$language.lang", sb.toString())
    }

    fun read(language: String) {
        val content = readFile("$language.lang")
        val lines = content.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        prompts.clear()
        for (line in lines) {
            val split = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            for (i in 1..<split.size) {
                sb.append(split[i])
            }
            if (split.size == 2) {
                prompts[split[0]] = sb.toString()
            }
        }
    }

    operator fun get(key: String): String {
        return prompts[key] ?: return key
    }

    fun put(name: String, content: String) {
        prompts[name] = content
    }
}

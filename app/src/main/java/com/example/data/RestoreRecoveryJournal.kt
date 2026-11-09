package com.example.data

import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** Durable preimage. An interrupted write preserves the previous journal rather than replacing it partially. */
internal class RestoreRecoveryJournal(directory: File) {
    private val file = AtomicFile(File(directory, "restore_recovery.json"))

    fun write(preimage: JSONObject) {
        val payload = preimage.toString()
        val record = JSONObject().put("version", 1).put("payload", payload)
            .put("sha256", com.example.domain.MarkdownNovelParser.computeSha256Hex(payload.toByteArray(Charsets.UTF_8)))
        val stream = file.startWrite()
        try {
            stream.write(record.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (failure: Throwable) {
            file.failWrite(stream)
            throw failure
        }
    }

    fun read(): JSONObject? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val record = JSONObject(String(file.readFully(), Charsets.UTF_8))
        require(record.getInt("version") == 1) { "Неизвестный формат журнала восстановления" }
        val payload = record.getString("payload")
        require(record.getString("sha256") == com.example.domain.MarkdownNovelParser.computeSha256Hex(payload.toByteArray(Charsets.UTF_8))) {
            "Повреждён журнал восстановления. Данные не изменены."
        }
        return JSONObject(payload)
    }

    fun clear() {
        file.delete()
        check(!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) { "Не удалось завершить журнал восстановления" }
    }
}

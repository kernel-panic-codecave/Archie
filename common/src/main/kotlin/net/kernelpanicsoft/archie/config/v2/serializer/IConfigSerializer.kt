package net.kernelpanicsoft.archie.config.v2.serializer

import java.nio.file.Files
import java.nio.file.Path
import net.kernelpanicsoft.archie.config.v2.model.ConfigDocument
import net.kernelpanicsoft.archie.config.v2.runtime.ConfigState

interface IConfigV2Serializer {
    fun configPath(document: ConfigDocument): Path

    fun load(document: ConfigDocument, state: ConfigState) {
        val path = configPath(document)
        if (Files.exists(path)) {
            try {
                val raw = Files.readString(path)
                loadString(document, state, raw)
            } catch (e: Throwable) {
                throw SerializationException(e)
            }
        }
        // Always save after load to materialize defaults/new keys.
        save(document, state)
    }

    fun loadString(document: ConfigDocument, state: ConfigState, raw: String)

    fun save(document: ConfigDocument, state: ConfigState) {
        val path = configPath(document)
        try {
            Files.createDirectories(path.parent)
            Files.writeString(path, saveString(document, state))
        } catch (e: Throwable) {
            throw SerializationException(e)
        }
    }

    fun saveString(document: ConfigDocument, state: ConfigState): String

    class SerializationException(cause: Throwable) : Exception(cause)
}


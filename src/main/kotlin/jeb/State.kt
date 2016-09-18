package jeb

import jeb.cfg.Json
import jeb.cfg.Parser
import jeb.cfg.toJson
import jeb.util.Try
import java.io.File

data class State constructor(
        val backupsDir: String,
        val source: List<String>,
        private val hanoi: Hanoi) {

    val lastTapeNumber = hanoi.largestDisc

    fun selectTape(): Pair<State, Int> {
        val hanoi = with(hanoi) {
            if (done) reset() else this
        }

        val (newHanoi, disk) = hanoi.moveDisk()

        return Pair(copy(hanoi = newHanoi), disk)
    }

    companion object {

        private val knownVersions = setOf("1".toJson(), "2".toJson())

        @JvmStatic fun loadState(configFile: File): Try<State> {
            val config = configFile.readText()
            val cfgJson = Parser.parse(config)
            if (cfgJson is Try.Failure) return Try.Failure(cfgJson.reason)

            val cfg = cfgJson.result
            if (cfg !is Json.Object) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val version = cfg["version"] ?: Json.String("1")
            if (version !is Json.String) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))
            if (version !in knownVersions) return Try.Failure(RuntimeException("Unknown version: ${version.value}"))


            val backupsJson = cfg["backupsDir"]
            if (backupsJson !is Json.String) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val sourceJson = cfg["source"] ?: return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))
            if (version.value == "1" && sourceJson !is Json.String) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))
            else if (version.value == "2" && sourceJson !is Json.Array) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val hanoiJson = cfg["hanoi"]
            if (hanoiJson !is Json.Object) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val hanoi = Hanoi.from(hanoiJson)
            if (hanoi is Try.Failure) return Try.Failure(hanoi.reason)

            val source: List<String> = when (sourceJson) {
                is Json.String -> listOf(sourceJson.value)
                is Json.Array -> sourceJson.toListOf<String>()
                else -> throw RuntimeException("Unknown source type: ${sourceJson.javaClass}")
            }

            return Try.Success(State(backupsJson.value, source, hanoi.result))
        }

        @JvmStatic fun saveState(configFile: File, state: State) {
            configFile.parentFile.mkdirs()
            val stateJson = Json.Object(linkedMapOf(
                    "version" to "2".toJson(),
                    "backupsDir" to state.backupsDir.toJson(),
                    "source" to state.source.toJson(),
                    "hanoi" to state.hanoi.toJson()))
            configFile.writeText(stateJson.toString())
        }
    }

}

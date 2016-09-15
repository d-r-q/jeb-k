package jeb

import jeb.cfg.Json
import jeb.cfg.Parser
import jeb.cfg.toJson
import jeb.util.Try
import java.io.File

data class State constructor(
        val backupsDir: String,
        val source: String,
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


        @JvmStatic fun loadState(configFile: File): Try<State> {
            val config = configFile.readText()
            val cfgJson = Parser.parse(config)
            if (cfgJson is Try.Failure) return Try.Failure(cfgJson.reason)

            val cfg = cfgJson.result
            if (cfg !is Json.Object) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val backupsJson = cfg["backupsDir"]
            if (backupsJson !is Json.String) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val sourceJson = cfg["source"]
            if (sourceJson !is Json.String) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val hanoiJson = cfg["hanoi"]
            if (hanoiJson !is Json.Object) return Try.Failure(RuntimeException("Invalid config file: \"$config\" (${configFile.absolutePath})"))

            val hanoi = Hanoi.from(hanoiJson)
            if (hanoi is Try.Failure) return Try.Failure(hanoi.reason)

            return Try.Success(State(backupsJson.value, sourceJson.value, hanoi.result))
        }

        @JvmStatic fun saveState(configFile: File, state: State) {
            configFile.parentFile.mkdirs()
            val stateJson = Json.Object(linkedMapOf(
                    "backupsDir" to state.backupsDir.toJson(),
                    "source" to state.source.toJson(),
                    "hanoi" to state.hanoi.toJson()))
            configFile.writeText(stateJson.toString())
        }
    }

}

package jeb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paranamer.ParanamerModule
import java.io.File

data class State @JsonCreator constructor(
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

        private val objectMapper = ObjectMapper().
                registerModule(ParanamerModule())

        @JvmStatic fun loadState(configFile: File): State {
            return objectMapper.readValue(configFile.readText(), State::class.java)
        }

        @JvmStatic fun saveState(configFile: File, state: State) {
            configFile.parentFile.mkdirs()
            val stateMap = mapOf(
                    "backupsDir" to state.backupsDir,
                    "source" to state.source,
                    "hanoi" to state.hanoi)
            configFile.writeText(objectMapper.writeValueAsString(stateMap))
        }
    }

}

package jeb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paranamer.ParanamerModule
import jeb.util.Try
import java.io.File

data class State @JsonCreator constructor(
        @JsonProperty("backupsDir") val backupsDir: String,
        @JsonProperty("source") val source: String,
        @JsonProperty("hanoi") private val hanoi: Hanoi) {

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

        @JvmStatic fun loadState(configFile: File): Try<State> {
            return try{
                Try.Success(objectMapper.readValue(configFile.readText(), State::class.java))
            } catch (e: JsonMappingException) {
                Try.Failure(e)
            }
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

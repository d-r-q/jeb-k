package jeb

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.io.File

class BackuperSpek : Spek() {init {

        val backups = "/tmp/backups"
        val source = "/tmp/source"

        given("Backup when hanoi is solved") {
            val io = Mockito.mock(Io::class.java)
            val state = State(backups, source, Hanoi(listOf(emptyList(), emptyList(), listOf(4, 3, 2, 1)), 15))
            val backuper = Backuper(io)
            on("perform backup") {

                `when`(io.latestDir("anything")).thenReturn(null)
                val newState = backuper.doBackup(state)
                it("should return correct new state") {
                    shouldEqual(newState.hanoi[0], listOf(4, 3, 2))
                    shouldEqual(newState.hanoi[1], listOf(1))
                }

                it ("should remove old data and create initial backup") {
                    val backup = File("/tmp/backups/1")
                    verify(io).remove(backup)
                    verify(io).copy(File(source), backup)
            }}}


    given("Directory with existing backup") {
        val io = Mockito.mock(Io::class.java)
        val state = State(backups, source, Hanoi(listOf(listOf(4, 3, 2),listOf(1), emptyList()), 1))
        val backuper = Backuper(io)
        on("perform backup") {

            val backup = File(backups, "1")
            `when`(io.latestDir(backups)).thenReturn(backup)
            val newState = backuper.doBackup(state)
            it("should return correct new state") {
                shouldEqual(newState.hanoi[0], listOf(4, 3))
                shouldEqual(newState.hanoi[1], listOf(1))
                shouldEqual(newState.hanoi[2], listOf(2))
            }

            it ("should remove old data and create initial backup") {
                val newBackup = File("/tmp/backups/2")
                verify(io).remove(newBackup)
                verify(io).sync(File(source), backup, newBackup)
            }}}


        given("Initial backup state") {
            val io = Mockito.mock(Io::class.java)
            val state = State(backups, source, Hanoi(listOf(listOf(4, 3, 2, 1), emptyList(), emptyList()), 0))
            val backuper = Backuper(io)
            on("perform backup") {
                `when`(io.latestDir("anything")).thenReturn(null)

                val newState = backuper.doBackup(state)
                it ("should return correct new state") {
                    shouldEqual(newState.hanoi[0], listOf(4, 3, 2))
                    shouldEqual(newState.hanoi[1], listOf(1))
                }
                it ("should remove old data and create initial backup") {
                    val backup = File("/tmp/backups/1")
                    verify(io).remove(backup)
                    verify(io).copy(File(source), backup)
                }}}}}

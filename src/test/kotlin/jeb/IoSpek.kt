package jeb

import jeb.ddsl.dir
import org.jetbrains.spek.api.*
import java.io.File

class IoSpek : Spek() {init {

    val jebDir = File("/tmp/jeb")

    given("Two nested directories with file within each and Io") {
        val outerDir = File(jebDir, "outerDir")
        val outerDirCopy = File(jebDir, "outerDirCopy")
        outerDir.deleteRecursively()
        outerDirCopy.deleteRecursively()

        val dir = dir(outerDir) {
            file("outerFile") { "content" }
            dir("nestedDir") {
                file("nestedFile") { "content2" }
            }
        }
        dir.create()

        val io = Storage()
        on("copy outer dir") {

            io.fullBackup(outerDir, outerDirCopy)

            it("copy should be equal") {
                shouldBeTrue(dir.contentEqualTo(outerDirCopy))
            }
            it("copy should be separate files") {
                shouldNotEqual(File(outerDir, "outerFile").inode, File(outerDirCopy, "outerFile").inode)
            }}}


    given("Directory with a file") {
        val from = File(jebDir, "mvFrom")
        val to = File(jebDir, "mvTo")
        from.deleteRecursively()
        to.deleteRecursively()

        val dir = dir(from) {
            file("file") { "content" }
        }
        dir.create()

        val io = Storage()
        on("move dir") {

            io.move(from, to)

            it("should be moved") {
                shouldBeFalse(from.exists())
                shouldBeTrue(dir.contentEqualTo(to))
            }}}


    given("""Origin directory with new, same and changed files
                Base directory with same, changed and deleted files
                Backup directory without files""") {

        val origin = File(jebDir, "origin")
        val base = File(jebDir, "base")
        val backup = File(jebDir, "backup")
        origin.deleteRecursively()
        base.deleteRecursively()
        backup.deleteRecursively()

        val originDir = dir(origin) {
            file("same") { "sameContent" }
            file("new") { "newContent" }
            file("changed") { "changedContent" }
        }
        originDir.create()

        dir(base) {
            file("same") { "sameContent" }
            file("changed") { "originContent" }
            file("deleted") { "deletedContent" }
        }.create()

        val io = Storage()
        on("sync dir") {
            io.incBackup(origin, base, backup)
            it("backup dir should be equal to origin") {
                shouldBeTrue(originDir.contentEqualTo(backup))
            }
            it("same should be hardlink and new should not be hardlink") {
                val originNew = File(origin, "new")
                val originSame = File(base, "same")
                val backupNew = File(backup, "new")
                val backupSame = File(backup, "same")
                shouldEqual(originSame.inode, backupSame.inode)
                shouldNotEqual(originNew.inode, backupNew.inode)
            }}}}}


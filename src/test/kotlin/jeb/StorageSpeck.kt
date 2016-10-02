package jeb

import jeb.ddsl.dir
import org.jetbrains.spek.api.*
import java.io.File

class StorageSpeck : Spek() {init {

    val jebDir = File("/tmp/jeb")

    given("Two nested directories with file within each") {
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

        val storage = Storage()
        on("copy outer dir") {

            storage.fullBackup(listOf(Source(outerDir.absolutePath + "/")), null, outerDirCopy)

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

        val storage = Storage()
        on("move dir") {

            storage.move(from, to)

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

        val storage = Storage()
        on("sync dir") {
            storage.incBackup(listOf(Source(origin.absolutePath + "/")), null, base, backup)
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
            }}}

    given("""Directories a and b with files (a1, a2) and (b1, b2) correspondly
             and Sources for that directories with type content and directory""") {
        val a = File(jebDir, "a")
        val b = File(jebDir, "b")
        val backup = File(jebDir, "backup2")
        a.deleteRecursively()
        b.deleteRecursively()
        backup.deleteRecursively()

        val aDir = dir(a) {
            file("a1") { "a1Content" }
            file("a2") { "a2Content" }
        }
        aDir.create()
        val bDir = dir(b) {
            file("b1") { "b1Content" }
            file("b2") { "b2Content" }
        }
        bDir.create()
        val aSource = Source(a, BackupType.CONTENT)
        val bSoource = Source(b, BackupType.DIRECTORY)

        val storage = Storage()
        on("sync dirs") {
            storage.fullBackup(listOf(aSource, bSoource), null, backup)
            it("backup directory should contain a1 and a2 files and b dir") {
                val res = dir(backup) {
                    dir("a") {
                        file("a1") { "a1Content" }
                        file("a2") { "a2Content" }
                    }
                    dir("b") {
                        file("b1") { "b1Content" }
                        file("b2") { "b2Content" }
                    }

                }
                res.contentEqualTo(backup)
            }}}

}
}



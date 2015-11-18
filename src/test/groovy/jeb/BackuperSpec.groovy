package jeb

import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SuppressWarnings("GroovyAssignabilityCheck")
class BackuperSpec extends Specification {

    private static final String now = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    public static final String backupsDir = "/tmp/backups"
    public static final String sourceDir = "/tmp/source"
    public static final File newBackupDir = new File(backupsDir, "$now-1")

    def "on initial backup, backuper should create new backup, remove old disk content and move backup into freed disk"() {

        given:
        def state = new State(backupsDir, sourceDir, new Hanoi(4))
        def io = Mock(Storage)
        io.lastModified(new File(backupsDir)) >> null
        io.fileExists(_) >> true
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:

        1 * io.copy(new File(sourceDir), { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    def "when in backups directory already exists some directory, backup should be created on base of this directory"() {

        given:
        def state = new State(backupsDir, sourceDir, new Hanoi(4))
        def io = Mock(Storage)
        def existingDir = new File(backupsDir, "$now-2")
        io.lastModified(new File(backupsDir), _) >> existingDir
        io.fileExists(_) >> true
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:

        1 * io.sync(new File(sourceDir), existingDir, { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    def "when hanoi is in solved state, backuper should reset it and continue"() {
        given:
        def state = new State(backupsDir, sourceDir, Hanois.createHanoi(4, 15))
        def io = Mock(Storage)
        io.lastModified(new File(backupsDir)) >> null
        io.fileExists(_) >> true
        def backuper = new Backuper(io)
        def newBackupDir = new File(backupsDir, "$now-1")

        when:
        def newState = backuper.doBackup(state)

        then:
        newState.hanoi.get(0) == [4, 3, 2]
        newState.hanoi.get(1) == [1]

        1 * io.copy(new File(sourceDir), { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    def "backuper should not remove old disk content, if new backup creation failed"() {
        given:
        def state = new State(backupsDir, sourceDir, new Hanoi(4))
        def io = Mock(Storage)
        io.lastModified(new File(backupsDir)) >> null
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:
        thrown(JebExecException)

        1 * io.copy(new File(sourceDir), _) >> {
            throw new JebExecException("cmd", "stdout", "stderr", 127, null)
        }
        0 * io.remove(newBackupDir)
        0 * io.move(_, _)
    }

    def "backuper should do nothing, if in backups directory exists subdirectory modified today"() {
        given:
        def state = new State(backupsDir, sourceDir, new Hanoi(4))
        def io = Mock(Storage)
        io.fileExists(_, _) >> true
        def backuper = new Backuper(io)

        when:
        def newState = backuper.doBackup(state)

        then:
        newState == state
        0 * io.copy(_, _)
        0 * io.sync(_, _, _)
        0 * io.remove(_)
        0 * io.move(_, _)
    }

    def "backuper should move old disk content to biggest disk if it's empty, instead of just removing it"() {

        def state = new State(backupsDir, sourceDir, Hanois.createHanoi(4, 2))
        def io = Mock(Storage)
        io.fileExists(_, _) >> false
        io.fileExists(new File(backupsDir, "$now-4")) >> false
        io.fileExists(new File(backupsDir, "$now-1")) >> true
        io.lastModified(_, _) >> new File(backupsDir, "$now-2")
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:
        1 * io.sync(new File(sourceDir), new File(backupsDir, "$now-2"), {
            it.absolutePath.startsWith("$backupsDir/$now-1-")
        })
        1 * io.move(new File(backupsDir, "$now-1"), new File(backupsDir, "$now-4"))
        1 * io.move({ it.absolutePath.startsWith("$backupsDir/$now-1-") }, new File(backupsDir, "$now-1"))
    }

    def "backuper should not prepare tape if it's not exists"() {

        given:
        def state = new State(backupsDir, sourceDir, new Hanoi(4))
        def io = Mock(Storage)
        io.fileExists(_, _) >> false
        io.fileExists(new File(backupsDir, "$now-4")) >> false
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:
        1 * io.copy(_, _)
        1 * io.move(_, new File(backupsDir, "$now-1"))

        0 * io.sync(_, _, _)
        0 * io.remove(_)
        0 * io.move(_, new File(backupsDir, "$now-4"))
    }

    static def tmpDir = File.createTempDir("jeb", "backuper-test")
    static {
        tmpDir.deleteOnExit()
    }

    @Unroll
    def "directory with name #name should be recognized as tape = #expectedIsTape"(String name, boolean expectedIsTape) {
        given:
        def f = new File(tmpDir, name)
        f.mkdirs()
        f.deleteOnExit()

        def s = new State("any", "any", Hanois.createHanoi(3, 0))

        when:
        def isTape = BackuperKt.isTape(f, s)

        then:
        isTape == expectedIsTape

        where:
        name           | expectedIsTape
        "20151118-1"   | true
        "20151118-3"   | true
        "55555555-2"   | true

        "20151118--1"  | false
        "20151118-0"   | false
        "20151118-4"   | false
        "20151118-nan" | false

        "20151118 -1"  | false
        "20151118 0"   | false
        "20151118 1"   | false
        "20151118 3"   | false
        "20151118 4"   | false
        "20151118 nan" | false

        "2015111--1"   | false
        "2015111-0"    | false
        "2015111-1"    | false
        "2015111-3"    | false
        "2015111-4"    | false
        "2015111-nan"  | false

    }
}

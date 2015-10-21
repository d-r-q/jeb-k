package jeb

import spock.lang.Specification

class BackuperSpec extends Specification {

    public static final String backupsDir = "/tmp/backups"
    public static final String sourceDir = "/tmp/source"
    public static final File newBackupDir = new File(backupsDir, "1")

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "on initial backup, backuper should create new backup, remove old disk content and move backup into freed disk"() {

        given:
        def state = new State(backupsDir, sourceDir, new Hanoi([[4, 3, 2, 1], [], []], 0))
        def io = Mock(Io)
        io.latestDir(new File(backupsDir)) >> null
        io.fileExists(_) >> true
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:

        1 * io.copy(new File(sourceDir), { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "when in backups directory already exists some directory, backup should be created on base of this directory"() {

        given:
        def state = new State(backupsDir, sourceDir, new Hanoi([[4, 3, 2, 1], [], []], 0))
        def io = Mock(Io)
        def existingDir = new File(backupsDir, "2")
        io.latestDir(new File(backupsDir)) >> existingDir
        io.fileExists(_) >> true
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:

        1 * io.sync(new File(sourceDir), existingDir, { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "when hanoi is in solved state, backuper should reset it and continue"() {
        given:
        def state = new State(backupsDir, sourceDir, new Hanoi([[], [], [4, 3, 2, 1]], 15))
        def io = Mock(Io)
        io.latestDir(new File(backupsDir)) >> null
        io.fileExists(_) >> true
        def backuper = new Backuper(io)
        def newBackupDir = new File(backupsDir, "1")

        when:
        def newState = backuper.doBackup(state)

        then:
        newState.hanoi.get(0) == [4, 3, 2]
        newState.hanoi.get(1) == [1]

        1 * io.copy(new File(sourceDir), { it.absolutePath.startsWith(newBackupDir.absolutePath) })
        1 * io.remove(newBackupDir)
        1 * io.move({ it.absolutePath.startsWith(newBackupDir.absolutePath) }, newBackupDir)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "backuper should not remove old disk content, if new backup creation failed"() {
        given:
        def state = new State(backupsDir, sourceDir, new Hanoi([[4, 3, 2, 1], [], []], 0))
        def io = Mock(Io)
        io.latestDir(new File(backupsDir)) >> null
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

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "backuper should do nothing, if in backups directory exists subdirectory modified today"() {
        given:
        def state = new State(backupsDir, sourceDir, new Hanoi([[4, 3, 2, 1], [], []], 0))
        def io = Mock(Io)
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

    @SuppressWarnings(["GroovyAssignabilityCheck", "GroovyAssignabilityCheck"])
    def "backuper should move old disk content to biggest disk if it's empty, instead of just removing it"() {

        def state = new State(backupsDir, sourceDir, new Hanoi([[4, 3], [1], [2]], 0))
        def io = Mock(Io)
        io.fileExists(_, _) >> false
        io.fileExists(new File(backupsDir, "4")) >> false
        io.latestDir(_) >> new File(backupsDir, "2")
        def backuper = new Backuper(io)

        when:
        backuper.doBackup(state)

        then:
        1 * io.sync(new File(sourceDir), new File(backupsDir, "2"), { it.absolutePath.startsWith("$backupsDir/1-")})
        1 * io.move(new File(backupsDir, "1"), new File(backupsDir, "4"))
        1 * io.move({ it.absolutePath.startsWith("$backupsDir/1-")}, new File(backupsDir, "1"))
    }

}

# Jeb
Jeb - it's incremental backup tool with exponentially increasing intervals between backups for Unix-based systems (rsync required)
It's fork of [aleksey-zhidkov/Introduce-Kotlin](https://github.com/aleksey-zhidkov/introduce-kotlin) and rewritten version of [aleksey-zhidkov/jeb](https://github.com/aleksey-zhidkov/jeb)

Original idea of incremental backup technique: [Incremental Backups on Linux](http://www.admin-magazine.com/Articles/Using-rsync-for-Backups)

## Usage
```bash
jeb-k <init|backup <dir-where-backup-is-stored>>
```
Where `init` start wizzard, that helps initialize new backup and `backup` executes backup of directory, that configured in config in given directory.

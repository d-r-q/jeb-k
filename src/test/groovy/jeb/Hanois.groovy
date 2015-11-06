package jeb

class Hanois {

    static Hanoi createHanoi(int disks, int step) {
        def hanoi = new Hanoi(disks)
        step.times {
            hanoi = hanoi.moveDisk().first
        }
        return hanoi
    }

}

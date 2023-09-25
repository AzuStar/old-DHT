package Shared.multipart;

/**
 * Special kind of lock, that does not explicitly lock object but just spits out
 * false or true.
 */
public class PassThroughLock {

    private boolean Locked = false;

    /**
     * Locks on block defined in wrapping if statement.
     * @return
     */
    public boolean lock() {
        synchronized (this) {
            if(Locked) return true;
            Locked = true;
            return false;
        }
    }
    /**
     * Unlocks block defined within if(this.lock())
     */
    public void unlock() {
        synchronized (this) {
            Locked = false;
        }
    }

}
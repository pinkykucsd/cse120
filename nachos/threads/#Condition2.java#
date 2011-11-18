package nachos.threads;
import nachos.machine.*;
import java.util.ArrayList;
/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see    nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param    conditionLock    the lock associated with this condition
     *                variable. The current thread must hold this
     *                lock whenever it uses <tt>sleep()</tt>,
     *                <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {   
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	// prevents interrupts from being used,
	boolean Status= Machine.interrupt().disable();
   
	conditionLock.release();
	// add current Thread to array list
	waitingThreads.add(KThread.currentThread());
	//put thread to sleep
	KThread.sleep();
	//restore status
	Machine.interrupt().restore(Status);
	//current thread must hold lock
	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
   
	//check if arraylist of waiting threads, that were put to sleep if there are any left, if so continue
	if (! waitingThreads.isEmpty())
	    {
		boolean Status = Machine.interrupt().disable();
   
		waitingThreads.remove(0).ready();
   
		Machine.interrupt().restore(Status);

	    }


    }


    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
   
	while (! (waitingThreads.isEmpty()))
	    {
		this.wake();
	    }

    }

    private Lock conditionLock;
    private ArrayList<KThread> waitingThreads = new ArrayList<KThread>();

}

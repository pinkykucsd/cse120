package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        //disable interrupts (maybe not necessary)
        Machine.interrupt().disable(); //let's see if this works? 
        //get current time
        long currentTime=Machine.timer().getTime();
        //go through waitQueue and put each thread that is "less than" current wait time on ready queue
        AlarmBucket placeholder=null;
        int count=waitQueue.size();
        while(count>0){
            placeholder=waitQueue.peek();
	    if(placeholder.getTime()<=currentTime){
		placeholder=waitQueue.poll();
                placeholder.getKThread().ready();
                count=waitQueue.size();
            }else{
		count=0;
            }
        }  
        //yield current thread
	KThread.currentThread().yield();
        //Machine.interrupt().enable(); //let's see if this works? DAC DEBUG
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        //disable interrupts
        Machine.interrupt().disable(); //let's see if this works? DAC DEBUG
        //calculate wakeTime
        long wakeTime = Machine.timer().getTime() + x;
        //create AlarmBucket
        AlarmBucket Abuck = new AlarmBucket(wakeTime, KThread.currentThread());
        //add current thread (alarm bucket) to waitQueue 
        waitQueue.add(Abuck);
        //put current thread to sleep
        KThread.currentThread().sleep();
        
        return;
    }
    
    //priority queue to store sorted threads
        private PriorityQueue<AlarmBucket> waitQueue = new PriorityQueue<AlarmBucket>();
    // ALARM thoughts:
    /*  
     * create a threadQueue? (other DS might be better - heap for instance) for the alarm class
     * put threads on this queue when waitUntil is called
     * sort threadQueue so that the soonest threads will go off at the beginning
     *  */
    
}

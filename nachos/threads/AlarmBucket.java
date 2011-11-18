package nachos.threads;

import nachos.machine.*;

/**
 * This clas defines a "bucket" which contains a pair of values, a Thread and the time it should be awoken.
 *
 * -Dan Cashman 10/15/11
 */

public class AlarmBucket implements Comparable<AlarmBucket> {
    /**
     * AlarmBucket:  constructor for the class.
     * Params:
     *  int time - the time after which to wake up the KThread
     *  KThread kthread - the thread to be woken up
     */
    public AlarmBucket(){ 
	this.time=0;
        this.kthread=null;
    }
    /**
     * AlarmBucket:  constructor for the class.
     * Params:
     *  int time - the time after which to wake up the KThread
     *  KThread kthread - the thread to be woken up
     */
    public AlarmBucket(long time, KThread kthread){ 
	this.time=time;
        this.kthread=kthread;
    }


    /**
     * getTime():  Gets the private integer value of the class which represents the time to wake up the thread
     * returns: int time
     */
    public long getTime(){
	return this.time;
    }
   
    /**
     * getKThread():  Gets the private KThread value of the class which represents the thread to be awoken
     * returns: KThread kthread
     */
    public KThread getKThread(){
	return this.kthread;
    }
    /**
     *compareTo(): compares this bucket to another bucket based on the time.  The smaller the time value, the smaller the Bucket
     *
     */
    public int compareTo(AlarmBucket Abuck){
	return (int) (this.time - Abuck.getTime());  //if this has a smaller "time" value, it will be sorted as less-than.
    }

    /*
     *private variables:  
     */
    private long time=0;
    private KThread kthread=null;
}
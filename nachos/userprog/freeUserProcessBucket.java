package nachos.userprog;
import nachos.threads.*;
import nachos.machine.*;

/**
 * This clas defines a "bucket" which contains a pair of values, a UserProcess PID and its exit status
 *
 * -Dan Cashman 10/15/11
 */

public class freeUserProcessBucket {
    /**
     * UserProcessBucket:  constructor for the class.
     * Params:
     *  int status - the status of the UserProcess
     *  int pid - the pid of the freed UserProcess
     */
    public freeUserProcessBucket(){ 
	this.status=0;
        this.pid=0;
    }
    /**
     * UserProcessBucket:  constructor for the class.
     * Params:
     *  int status - the status of the UserProcess
     *  UserProcess - the user process
     */
    public freeUserProcessBucket(int status, int pid){ 
	this.status=status;
        this.pid=pid;
    }




    /**
     * getStatus(): blah
     * returns: int status
     */
    public int getStatus(){
	return this.status;
    }
   
    /**
     * getUserProcess(): blah
     * returns: UserProcess process
     */
    public int getPID(){
	return this.pid;
    }

    /*
     *private variables:  
     */
    private int status=0;
    private int pid=0;
}
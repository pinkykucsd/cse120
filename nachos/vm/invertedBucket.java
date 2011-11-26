package nachos.vm;

import nachos.machine.*;

/**
 * This clas defines a "bucket" which contains a translation entry, a used bit, a VMProcess, a pinned bit, and an assigned bit.
 *
 * -Dan Cashman 11/23/11
 */

public class invertedBucket {
    /**
     * invertedBucket:  constructor for the class.
     * Params:
     *  none
     */
    public invertedBucket(){ 
	this.tEntry=null;
        this.used=false;
        this.pinned=false;
        this.assigned=false;
        this.process=null;
    }
    /**
     * invertedBucket:  constructor for the class.
     * Params:
     *  TranslationEntry tEntry 
     *  boolean used
     *  boolean assigned
     *  VMProcess process
     */
    public invertedBucket(TranslationEntry tEntry, boolean used, boolean pinned, boolean assigned, VMProcess process){ 
	this.tEntry=tEntry;
        this.used=used;
        this.pinned=pinned;
        this.assigned=assigned;
        this.process=process;
    }

 
    /*
     *private variables:  
     */
    public TranslationEntry tEntry;
    public boolean used;
    public boolean pinned;
    public boolean assigned;
    public VMProcess process;

}
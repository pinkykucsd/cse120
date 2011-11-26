package nachos.vm;

import nachos.machine.*;

/**
 * This clas defines a "bucket" which contains a translation entry, a corresponding section, and an index to the page in that section
 *
 * -Dan Cashman 11/23/11
 */

public class ptBucket {
    /**
     * ptBucket:  constructor for the class.
     * Params:
     *  none
     */
    public ptBucket(){ 
        this.section=null;
        this.sectionPage=-1;   //not sure if this is ok, should be, DAC DEBUG
    }
    /**
     * ptBucket:  constructor for the class.
     * Params:
     *  section - the coff section associated with this page
     *  sectionPage -the index of the page for this vpn in the section
     */
    public ptBucket(TranslationEntry tEntry, CoffSection section, int sectionPage){ 
        this.section=section;
        this.sectionPage=sectionPage;
    }

    /*
     *private variables:  
     */
    public CoffSection section;
    public int sectionPage;

}
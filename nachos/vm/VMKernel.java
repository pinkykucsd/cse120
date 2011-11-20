package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
        //need to initialize the VMKernel slightly differently: need an inverted page table, a swap file, different locks, 
	super.initialize(args);  //need to modify super.initialize() into components as described in section to "cut out" the bad parts, but only for VMKernel DAC
        //initialize swap file         
        //initialize invertedPageTable 
        //more... 
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /**
     * scanForFreePage - 
     * params: none
     * returns - ppn of available page, or -1 if all physical pages are being used
     */
    private int scanForFreePage() {
        //code
        //can probably just use the clockAlgorithm here, but have as a separate function to make conceptualization easier
        //scan invertedPageTable looking for page with valid == 0 (all should have valid set to 0 before being used)
        //use currentPage "pointer" as starting poing (same as clock alg, again can probably just use that)
        //return ppn of available page
        //else
        return -1;
    }
    /**
     * assignPage - changes values in invertedPageTable to assign to process with id pid
     * params:
     *   ppn - the page in invertedPageTable to modify
     *   pid - the process id to assign to that page
     * returns - n/a
     */
    private void assignPage() {
        //code
        return;
    }
    /**
     * savePage - save page to swap if it needs to be, tell process shit went down
     * params:
     *   ppn - the page in invertedPageTable to save/zero out
     *   pid - the process id to of the process that owns this page
     * returns - n/a
     */
    private void savePage(int ppn, int pid) {
        //code
        //figure out if page needs to be saved to swap
        //if needs to be saved to swap, call addToSwap(ppn)
        //tell process with pid that the page with virtual address of (whatever the virtual address was for that page - look up in invertedPageTable)
        //    is no longer valid, and if is in swap file, give the page number in swap file (returned by addToSwap() )
        return;
    }
    /**
     * addToSwap - adds page from invertedPageTable to swap file for storage
     * params:
     *   ppn - the page in invertedPageTable to copy to swap file
     * returns - the page in swap file where it was saved
     */
    private int addToSwap(int ppn) {
        //code
        //get next free page from freeSwap.  (consider case when there is no page in freeSwap as well, need to get new page from swap file)
        //Copy physical page ppn to page of freeSwap just grabbed
        //return the page in swap that it has been saved to
        return 0; //place holder
    }
     /**
     * evictPage - kicks a page in physical memory out and notifies the process that owns it
     * params:  none
     * returns: ppn of page evicted and ready for use
     */
    private int evictPage() {
	    //call clockAlgorithm() to figure out which page to get rid of, might be put to sleep if all pages are 'pinned'
	    //call savePage() to save physical page to swap if necessary and notify process that its page is invalid 'n' stuff
            //call zeros() to zero out page?  (maybe not necessary, don't worry about for now)
            //returns ppn of newly-available page in invertedPageTable
        return 0;  //placeholder
    }
     /**
     * clockAlgorithm - determines which page should be evicted, if no pages can be evicted (all pinned) put caller to sleep.
     * params:  none
     * returns: ppn of page chosen to be evicted
     */
    private int clockAlgorithm() {
        //start at currentPage
        //set starting point to currentPage
        //if currentPage used bit ==0 and pinned==0, it is ok to evict, return this one
        //else, set currentPage used bit to 0, if currentPage is not pinned, set "notPinned" to true (this will be needed to prevent infinite loop of clock)
        //currentPage++
        //go around until a page can be evicted OR, if notPinned==false after it went to every page (currentPage-startpage==numPhysPages), then every
        //    page is pinned, put process to sleep
        //eventually return the page that should be evicted/used
        return 0;  //placeholder
    }
    //DAC
    /****************************************************************************************************************************************
     *getAvailablePage() - returns a page in physical memory which is available for use by a process.  Called when a process needs a page due
     *                      to an "invalid" bit set in their pageTable.  If there aren't any available pages, calls evictPage() to free one
                            and returns it.
     *params:
     *  int pid - the pid of the process calling this function (the process asking for the page)
     *returns - the next available page
     *****************************************************************************************************************************************/
    public int getAvailablePage(){
	//scanForFreePage() - scan invertedPageTable for available page 
	     //(might want to just use clock algorithm here too)
	     //(might also want to keep a "pointer"  (an int) that points to the next page, but this can be ignored for now
             //returns ppn of a free page, if there is one, else -1 or something

        //if there is an available page, assignPage(ppn,pid)
	   //assignPage(ppn,pid) - record appropriate values into TranslationEntry of invertedPageTable for page ppn,
	   //                      record PID of process to which page is assigned

        //else if there is not an available page returned by scanForFreePage(), call evictPage()
	    //evictPage() - calls clockAlgorithm() to figure out which page to get rid of,
	    //              calls savePage(ppn) to save physical page to swap if necessary and notify process that its page is invalid 'n' stuff
            //              calls zeros(ppn)to zero out page?  (maybe not necessary, don't worry about for now)
            //              returns ppn of newly-available page in invertedPageTable
            //assignPage(ppn, pid) - as above, associate the now-available page with the right values and owner
	//return the ppn found in 
        return 0;  //placeholder for now
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    /********************************************************************************************************************************************
     * DAN AND PINKY'S ADDED VARIABLES
     *******************************************************************************************************************************************/
    private OpenFile swapFile;   //The swap file  
    private LinkedList<TranslationEntry> invertedPageTable; //might need to make "buckets" so we can keep track of PID, but for now will use ppn for pid since it doesn't seem to have a purpose if we index into this table by ppn
    private int currentPage=0;  //the page currently pointed to by the clock algorithm
    private priorityQueue<Integer> freeSwap; //heap which returns lowest-valued free page in swap file, some more design needed here for managing memory, removing blocks, etc.  Should ask how dynamic freeing of the memory needs to be. (do we do as malloc, etc)
}

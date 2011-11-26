package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Random;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        //make all entries in TLB invalid (they all refer to this process's virtual address space)   //DAC
        //might need to save a copy of TLB for use upon restore
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
	    TranslationEntry entry = Machine.processor().readTLBEntry(i);
	    if (entry.valid) {
                //update vpn with values (probably necessary) and update invertedPageTable too
		pageTable[entry.vpn].dirty = entry.dirty;     
		pageTable[entry.vpn].used = entry.used;
                VMKernel.invertedPageTable[entry.ppn].used=entry.used;    //DAC   
		entry.valid = false;
		Machine.processor().writeTLBEntry(i, entry);  //Unnecessary?? DAC DEBUG
	    }
	}
	super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        //might need to restore a saved copy of TLB?
	super.restoreState();     //might need to undo the "setPageTable" command in super since we are no longer using a pageTable in processor? DAC ???
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        //Load pageTable with a bunch of invalid pages that have not been written (valid = 0 and dirty = 0) (might start with NULL, but invalid is choice for now)  DAC
	return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        //this method might be less necessary than before, but haven't yet analyzed DAC
	super.unloadSections();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
        case Processor.exceptionTLBMiss:
	    //Add code for TLBMiss DAC
            int badAddr=Machine.processor().readRegister(Processor.regBadVAddr);   //read bad address from designated register
            int vpn = Processor.pageFromAddress(badAddr);                  //get virtual page from badAddr
            //invalid VPN in the first place
	    if (vpn < 0 || vpn >= pageTable.length){
                //return false;  gotta do something here, but what?  DAC DEBUG!!! 
            }
            if(checkValidAddress(vpn)==false){
                int newPage = VMKernel.getAvailablePage(this, vpn);   //gimme a page to use biznitch!  DAC
                initializePage(newPage,vpn);   //initialize page with information
            }
            addToTLB(vpn);   //add entry to TLB
	            //if read-only coff, get from load()/loadSections()/wherever I find that info
                    //if on disk, call VMKernel.getPageFromSwap(spn) (get page from swap file (info maintained in Kernel) from page spn in swap file)
	                 //spn will be stored in VMProcess.pageTable as a physical page number.  VMProcess will know it is on swap if dirty ==1 and valid==0
                    //if not yet loaded/written to, just use blank page (do we need to zero out the page when it is assigned from kernel?)
	        //addToTLB() (see above)
            
            break;
        
	default:
	    super.handleException(cause);
	    break;
	}
    }
    /**************************************************************************************************************************************************
     * updateTLB -update TLB so that entry with vpn (if exists) is valid/valid 
     * params:
     *   int vpn - the vpn to search for in TLB, set this entry to invalid
     *   boolean valid - value to set valid
     * returns - n/a
     *****************************************************************************************************************************************************/
    private void updateTLB(int vpn, boolean valid) {
        TranslationEntry entry;
	for (int i = 0; i < Machine.processor().getTLBSize(); i++){
             entry = Machine.processor().readTLBEntry(i);
             if (entry.vpn==vpn){   
                 entry.valid=valid;
             }
        }
       return;
   }
    /**********************************************************************************************************************************************
     * savePage - save page to swap if it needs to be, tell process shit went down
     * params:
     *   ppn - the page in invertedPageTable to save/zero out
     * returns - n/a
     *********************************************************************************************************************************************/
   public void savePage(int vpn) {
        //figure out if page needs to be saved to swap
       TranslationEntry tEntry=pageTable[vpn];   //DAC DEBUG - make sure that
       if(tEntry.dirty){    //entry has been written to, save to swap
           int swapLocation=VMKernel.addToSwap(tEntry.ppn);//add to swap
           pageTable[vpn].ppn=swapLocation;  //make physical location swap loaction
       }
       pageTable[vpn].valid=false;  //this is no longer a valid mapping
       updateTLB(vpn, false);  //make sure TLB knows this is now invalid
       return;
   }

    /*************************************************************************************************************************************
     * checkAddress():  scans page table of VMProcess to see if there is a valid page 	
     * params:
     *    vAddr - the virtual address to check
     * return - true if address is valid
     ************************************************************************************************************************************/
    //checkAddress()
    //get virtual page from badAddr
    //get entry corresponding to that vpn in pageTable in this process
    //return whether or not "valid"
    public boolean checkValidAddress(int vpn){
	//get entry corresponding to that vpn in pageTable in this process
	TranslationEntry entry = pageTable[vpn];
	if(entry.valid){
	    return true;
	}else{
	    return false;
        }
    }

    /*************************************************************************************************************************************
     * addToTLB: adds translation entry to TLB
     * params:
     *    vAddr - the virtual address of the entry to add
     * return - void
     ************************************************************************************************************************************/
    public void addToTLB(int vpn){
        //PK:checkinf for free entry.
	// stores the Id/entry of the trasnlation entry that will be replaced
	int freeEntry = -1;
	// looping through TLB table, trying to find free index to write to
	//checking if full
	for (int i = 0; (i < Machine.processor().getTLBSize()) && (freeEntry == -1); i++){
		TranslationEntry entry = Machine.processor().readTLBEntry(i);
		if (!entry.valid){
		    freeEntry = i;
                }
	}
	// PK: if no free entry was found, we replace by Random b/c TLB is full
	if (freeEntry == -1){
    	    freeEntry = randomGenerator.nextInt(Machine.processor().getTLBSize());   //get the next random number 
	}//end of IFstatement of no free entry found
        //write to tlb at appropriate spot with entry from pageTable
	Machine.processor().writeTLBEntry(freeEntry, pageTable[vpn]);    //assumes pageTable[vpn] is set and valid, DAC DEBUG
    }//end of addtoTLB




	    
     
    /*************************************************************************************************************************************
     * initializePage: fill page with appropriate data
     * params:
     *    int pageFromKern - physical page to fill w/appropriate data
     *    int vpn - the virtual page we're interested in
     * return - void
     ************************************************************************************************************************************/
    //initializePage(getspagefromkernel) (initialize page with appropriate information)

    public void initializePage(int pageFromKern, int vpn){
        int ppn = pageFromKern;
	boolean executablePage=false; 
        
        return;
    }
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    private static Random randomGenerator = new Random();
    private ptBucket[] coffMap;
    //not sure if we need to redeclare variables that are in the parent class   DAC DEBUG ???
    
}

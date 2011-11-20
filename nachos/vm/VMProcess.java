package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

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
	super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
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
            //checkAddress()
                //get virtual page from badAddr
                //get entry corresponding to that vpn in pageTable in this process
                //return whether or not "valid"
            //if(checkaddress()===valid)..
	        //addToTLB() (add translation entry to TLB)
	            //kick out random entry (maybe make sure TLB is full first)
	            //put entry into tlb with valid bit set
	    //else
	        //call VMKernel.getAvailablePage()  (get a page of physical memory from Kernel, see getAvailablePage() in VMKernel)
                //initializePage() (initialize page with appropriate information)
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
    /*************************************************************************************************************************************
     * checkAddress():  scans page table of VMProcess to see if there is a valid page 	
     * params:
     *    vAddr - the virtual address to check
     * return - true if 
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    //not sure if we need to redeclare variables that are in the parent class   DAC DEBUG ???
    
}

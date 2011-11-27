package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;   //AFTER MERGE

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	//	pageTable[i] = new TranslationEntry(i,i, true,false,false,false);   //this is doing the 1-1 mapping methinks  DAC  */  //AFTER MERGE2
        UserKernel.numProcessLock.acquire();
        UserKernel.numActiveProcesses++;  //AFTER MERGE2  
        UserKernel.numProcessLock.release();
        //UserKernel.assignPID(this);
        this.pid=global_pid++;
        System.out.print("Creating a process\n with PID:\t"+pid+"\n");  //TRACE
	//initialize file_descriptors 
          file_descriptors= new OpenFile[NUM_FD];    //array of fileDescriptors for this process
          file_descriptors[0] = UserKernel.console.openForReading();  //stdin
          Lib.assertTrue(file_descriptors[0]!=null, "StdIn does not exist\n");    //DAC DEBUG
          file_descriptors[1] = UserKernel.console.openForWriting();  //stdout
          Lib.assertTrue(file_descriptors[1]!=null, "StdOut does not exist\n");   //DAC DEBUG
	  //	  testFileSystem();   //test filesystem (see bottom)
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        //System.out.print(args.length);       //TRACE
	//        for(int i=0;i<args.length;i++)
        //System.out.print("name:\t"+name+"\targs:\t"+args[i]+"\n");   //TRACE
		if (!load(name, args)){
		    System.out.print("Failed to Load in Execute\n");  //DEBUG
	          return false;
		}
	
	new UThread(this).setName(name).fork();  //AFTER MERGE2 : this differs from Pinky's version she declares myThread.  leave as-orig for now

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {

	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	//	System.out.print("readVirtualMemory\n");   //TRACE
        if(offset<0||length<0||offset+length>data.length){
            System.out.print("Invaldi args to readVirtualMemory\n");   //AFTER MERGE2 DAC DEBUG
	    return 0;
        }

	byte[] memory = Machine.processor().getMemory();
	
	/*// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);

	return amount;  */  //AFTER MERGE2 original code above, Pinky's below
        if (vaddr<0||vaddr>=pageTable.length*(pageSize-1))    //changed this to pageTable.numPages*(pageSize-1)(verify) DAC DEBUG  AFTER MERGE2  
	    return 0;
        //declaring here just because AFTER MERGE2 DAC DEBUG
        int byteNum=0;
        int pageIndex=0;
        int pageOffset=0;
        int bytesLeftInPage=0;
        int bytesToRead=0;
        int physicalAddr=0;
        do{
            pageIndex = Processor.pageFromAddress(vaddr + byteNum);             //get the page it thinks it is on
            if(pageIndex<0||pageIndex>=pageTable.length)
		return 0;
            pageOffset=Processor.offsetFromAddress(vaddr+byteNum);    //get offset from page base of this virtual address
            bytesLeftInPage = pageSize- pageOffset;                   //number of bytes you can still write on this page
            bytesToRead = Math.min(bytesLeftInPage, length-byteNum);  //seee how many bytes you can read from this page before page change
            physicalAddr= pageTable[pageIndex].ppn*pageSize+pageOffset;//get corresponding physical address
            System.arraycopy(memory, physicalAddr, data, offset + byteNum, bytesToRead);
            byteNum+= bytesToRead;
	}while(byteNum<length);
            return byteNum;  //total written
    }


    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	//	System.out.print("WriteVirtualMemory\n");  //TRACE

	//AFTER MERGE2  DAC DEBUG - creating my own writeVirtualMemory()

        if(offset<0||length<0||offset+length>data.length){
            System.out.print("Invaldi args to writeVirtualMemory\n");   //AFTER MERGE2 DAC DEBUG
	    return 0;
        }

	byte[] memory = Machine.processor().getMemory();
        if(pageTable==null){
            System.out.print("pageTable not yet initialized\n");  //AFTER MERGE2 DAC DEBUG
        }
        if (vaddr<0||vaddr>=pageTable.length*(pageSize-1))    //changed this to pageTable.numPages*(pageSize-1)(verify) DAC DEBUG  AFTER MERGE2  
	    return 0;
        //declaring here just because AFTER MERGE2 DAC DEBUG
        int byteNum=0;
        int pageIndex=0;
        int pageOffset=0;
        int bytesLeftInPage=0;
        int bytesToWrite=0;
        int physicalAddr=0;
        do{
            pageIndex = Processor.pageFromAddress(vaddr + byteNum);             //get the page it thinks it is on
            if(pageIndex<0||pageIndex>=pageTable.length)
		return 0;
            pageOffset=Processor.offsetFromAddress(vaddr+byteNum);    //get offset from page base of this virtual address
            bytesLeftInPage = pageSize- pageOffset;                   //number of bytes you can still write on this page
            bytesToWrite = Math.min(bytesLeftInPage, length-byteNum);  //seee how many bytes you can read from this page before page change  //again to Integer Object DAC DEBUG
            physicalAddr= pageTable[pageIndex].ppn*pageSize+pageOffset;//get corresponding physical address
            System.arraycopy(data, offset+byteNum, memory, physicalAddr, bytesToWrite);  //AFTER MERGE2 DAC DEBUG (make sure this is right)
            byteNum+= bytesToWrite;
	}while(byteNum<length);
        return byteNum;  //total written

    }




    
    /***************************
     *PINKY HAS getPhysicalPage() here, doesn't seem necessary at present  AFTER MERGE2 DAC DEBUG
     ***************************/

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
        //open the file
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}
        //make a coff
	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");  
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections()){  //AFTER MERGE2 DAC DEBUG
            System.out.print("Load Sections not Valid\n");
	    return false;
        }
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;
	this.argc = args.length;
	this.argv = entryOffset;
	// store arguments in last page 
	storeArguments(argv, args, entryOffset, stringOffset);
        /*    store arguments part	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	} */

	return true;
    }
    /*************************************************************************************************************
     * storeArguments() - load the arg page with the appropriate information (copied line-by line from inside load
     * params:
     *   none
     * returns - N/A
     *************************************************************************************************************/
    protected void storeArguments(byte[][] argv, String[] args, int entryOffset, int stringOffset){
       	// store arguments in last page
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}
	return;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into   AFTER MERGE2 DAC DEBUG : potential issue, loading coff secs into wrong pages top v botom
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {

         if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
        UserKernel.pageLock.acquire();
	// load sections
        while(numPages>UserKernel.freePagesPool.size()){   //AFTER MERGE2 DAC DEBUG - need corresponding WAKE call from exit  DEBUG2 why size() here and length in readVirtualMemory and writeVirtualMemory?
            //go to sleep to wait for free pages
            System.out.print("Process "+pid+" going to sleep bcause "+numPages+"are not available, only "+UserKernel.freePagesPool.size()+"are available\n");//DEBUG
            UserKernel.processWait.sleep();  //woken up in unloadSections 
	}    
        //allocate pageTable with appropriate amount of paged initialized in load                                                                    
	pageTable = new TranslationEntry[numPages];

	//i = this processes virtual page number and each will have a physical page (page)                                                           
	for (int i=0; i < numPages; i++)
	    {
		int page = UserKernel.freePagesPool.removeFirst();
		pageTable[i] = new TranslationEntry(i,page, true,false,false,false);
	    }
	UserKernel.pageLock.release();

	// load sections                                                                                                                             
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);

	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		//int ppn = UserKernel.availablePages.remove().ppn;                                                                          
		pageTable[vpn].readOnly = section.isReadOnly();

		//section.loadPage(i, getPhysicalPage(vpn,false));                                                                           
		//ppn = physical page number                                                                                                 
		section.loadPage(i, pageTable[vpn].ppn);

	    }
	}

	return true;                               //AFTER MERGE2 DAC DEBUG - load sections w/cond 
    }




    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	//        System.out.print("Unload Sections\n");   //TRACE
        //traverse though processes virtual pages and free up the physical page associated with it                                                   
	UserKernel.pageLock.acquire();
	//System.out.print(UserKernel.freePagesPool.size()+"\n");
	for (int i = 0; i < numPages; i++ )
	    {
		//      System.out.print("i:\t"+i+"\t"+numPages+"\n");
		UserKernel.freePagesPool.add(pageTable[i].ppn);
		pageTable[i] = null;
	    }
	//	System.out.print(UserKernel.freePagesPool.size()+"\n");
	//alert waiting process that new pages are available
        UserKernel.processWait.wakeAll();               //AFTER MERGE2 DAC DEBUG - should be able to just wake up all and let them go back to sleep if cond not met
	UserKernel.pageLock.release();
        return;

    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }
    /*
     *return a free (null) file descriptor from file_descriptors[]
     */
    private int getNextFileDescriptor(){
	//modify this to increase efficiency (move to next-file descriptor rather than starting at 0 each time)
        for(int i=0; i<NUM_FD;i++){         //instead of hard-coding 16, maintain as private 'const'
	    if(file_descriptors[i]==null){
		return i;
	    }
        }
        return -1;   // there are no free fds (return -1)
    }
    /*
     *returns the first file descriptor with that name, or -1 if none
     */
    private int searchNameFileDescriptor(String name){
	//modify this to increase efficiency (move to next-file descriptor rather than starting at 0 each time)
        for(int i=0; i<NUM_FD;i++){         //instead of hard-coding 16, maintain as private 'const'
            if(file_descriptors[i]!=null){  
         	    if(file_descriptors[i].getName()==name){
		        return i;
	             }
            }
        }
        return -1;   // there are no free fds (return -1)
    }
  
      
    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    /**
     * Handle the "creat()" system call
     *  param: 
     *   int filename_address: the address in virtual memory of the filename to open/create
     */
    private int handleCreate(int filename_address) {
        //make sure there is an available file descriptor
        int fd = getNextFileDescriptor();
        if(fd==-1){
	    return -1;
        } 
        //sanitize memory address?  BULLET PROOF
        //read string from memory into buffer
        String filename = readVirtualMemoryString(filename_address, 256);  //DAC TO DO: make sure maxlength is appropriate(might need to be less than 256)
        //sanitize string BULLET PROOF
        //create OpenFile to hold about to be created file 
        OpenFile file = ThreadedKernel.fileSystem.open(filename, true);
        //put open file into array of file descripters
        file_descriptors[fd]=file;
        // return file descriptor
	return fd;           
    }

    /**
     * Handle the "open()" system call
     *  param: 
     *   int filename_address: the address in virtual memory of the filename to open/create
     */
    private int handleOpen(int filename_address) {
        //make sure there is an available file descriptor
        //if there is no available file descriptor, should we put the thread to sleep?  What if there is only one thread in the process DAC ???
        int fd = getNextFileDescriptor();
        if(fd==-1){
	    return -1;
        } 
        //sanitize memory address?  BULLET PROOF
        //read string from memory into buffer
        String filename = readVirtualMemoryString(filename_address, 256);  //DAC TO DO: make sure maxlength is appropriate(might need to be less than 256)
        //sanitize string BULLET PROOF
        //create OpenFile to hold about to be created file 
        OpenFile file = ThreadedKernel.fileSystem.open(filename, false);
        if(file==null){
	    return -1;
        }
        //put open file into array of file descripters
        file_descriptors[fd]=file;
        // return file descriptor
	return fd;           
    }
    private int handleRead(int fd, int buffer_address, int num_bytes) {
        //sanitize check fd
        if(fd<0||fd>15||file_descriptors[fd]==null){
            System.out.print("Invalid File Descriptor in hanldeRead()\n");
	    return -1;
        }
	//sanitize buffer_address DAC BULLET PROOF
        //make sure num_bytes is acceptable (doesn't cause read too far, positive, etc) DAC BULLET PROOF
        int offset=0;     //offset to read into buffer
        int buffSize=4096;
        byte[] buffer= new byte[buffSize];
        int toRead=0;
        int remaining_bytes=num_bytes;
        int total_read=0;
        int read=0;
        int written=0;
        while(remaining_bytes>0){
        	toRead=Math.min(buffSize,remaining_bytes);
		//           System.out.print("toRead Amount:\t"+toRead+"\n");  DAN DEBUG
                //System.out.print("Reading into readBuffer "+toRead+" bytes\n");
                read=file_descriptors[fd].read(buffer, 0, toRead);
		//                System.out.print("After read in OpenFile.read():\t" + read + "bytes read\n");   //DAC DEBUG
                if(read<0){    //something went wrong
                    System.out.print("read less than 0, something fishy up in hanldeRead\n");
		    return -1;
                }
		//                System.out.print("Attempting to write read material to virtual address\n");
                written=writeVirtualMemory(buffer_address, buffer, 0, read);
                if(read!=written){        
		    //    System.out.print("Something wrong with writeVirtualMemory in handleRead()\n");
		    return -1;            //something went wrong?
                } 
                total_read+=read;
                remaining_bytes-=read;       
                buffer_address+=read;
                if(read<toRead){
		    break;   //break out, no more to read     
		}
	}
	//        System.out.print("Total Read from Read:\t"+total_read+"\n");
        return total_read;          //return the total number of bytes read
                                    //better make sure all corne cases are dealt with  BULLET PROOF DAC  
    }
        
        
    private int handleWrite(int fd, int buffer_address, int num_bytes) {
        //sanitize check fd
        if(fd<0||fd>15||file_descriptors[fd]==null){
            System.out.print("Invalid File Descriptor in hanldeWrite()\n");
	    return -1;
        }
	//sanitize buffer_address DAC BULLET PROOF
        //make sure num_bytes is acceptable (doesn't cause read too far, positive, etc) DAC BULLET PROOF
        int offset=0;     //offset to read into buffer
        int buffSize=4096;
        byte[] buffer= new byte[buffSize];
        int toWrite=0;
        int remaining_bytes=num_bytes;
        int total_written=0;
        int read=0;
        int written=0;
        while(remaining_bytes>0){
        	toWrite=Math.min(buffSize,remaining_bytes);
                read=readVirtualMemory(buffer_address, buffer, 0, toWrite);  //make sure reads appropriate mem BULLET PROOF
		//      System.out.print("After read in handleWrite() readVirtualMemory returned:\t" + read + "\n");  //DAC DEBUG             
                written=file_descriptors[fd].write(buffer, 0, read);
                if(read!=written){
		    //  System.out.print("Read and written are different in writeHandle()\n");
                    return -1;
                }
                if(toWrite!=written){
		    //		    System.out.print("Didn't write all bytes in writeHandle()\n");
                    return -1;
                }
                total_written+=written;
                remaining_bytes-=written;
                buffer_address+=written;
        }
	//        System.out.print("Total Written from Written:\t"+total_written+"\n");
        return total_written;  
                                    //better make sure all corne cases are dealt with  BULLET PROOF DAC  
    }

    /*
     * close(): Frees a file descriptor
     */
     private int handleClose(int fd){
        //sanitize check fd
        if(fd<0||fd>15){
	    return -1;
	}else if(file_descriptors[fd]==null){
	    return -1;  //should this be 0 or -1?
        }else{
            file_descriptors[fd].close();
	    file_descriptors[fd]=null;   
	}
        return 0;
     }

    /*
     * unlink(): Frees a file descriptor and removes the file
     */
     private int handleUnlink(int filename_address){
        //sanitize check string address
          String filename = readVirtualMemoryString(filename_address, 256);  //DAC TO DO: make sure maxlength is appropriate(might need to be less than 256)
          int i=searchNameFileDescriptor(filename);
          while(i>-1){ 
	      handleClose(i);
              i=searchNameFileDescriptor(filename);
	  }
          //close file descriptors which have this as name
            System.out.print("trying to remove: "+filename+"\n");
            boolean killed=ThreadedKernel.fileSystem.remove(filename);   //kill it!
            System.out.print("Killed succesfully = :"+ killed+"\n");
        return 0;
     }


    //AFTER MERGE3 DAC EXEC
    private int handleExec(int filename_addr, int count, int arg_addr){   //DAC                                                                              
        //sanitize addresses                                                                                                                                 
        //get string from filename_adddress                                                                                                                  
        if(count<0){
            System.out.print("Cannot have negative arguments\n");
            return -1;
        }
        String filename = readVirtualMemoryString(filename_addr, 256);  //DAC TO DO: make sure maxlength is appropriate(might need to be less than 256)
	//       System.out.print("Filename in exec:\t"+filename+"Count:\t"+count+"\n");  //TRACE
        //make sure filename is appropriate                                                                                                                 
	/*	        if(filename.length()<6){
            System.out.print("Filename must be appropriate .coff file\n");
            return -1;
	    }
        String testCoff=filename.substring(filename.length()-6,filename.length()-2);  //DAC DEBUG maybe do without for testing
	   if(testCoff!=".coff"){
            System.out.print("Filename must be appropriate .coff file\n");
            return -1;
	    }   */
        //get strings from arguments by iterating through "count times" and put in a string array                                                            
        //get address of each before getting string                                                                                                         
            int arg_num=0;
            byte[] pointer_buff=new byte[4*count];
            String[] args=new String[count];
        if(count!=0){ 
            readVirtualMemory(arg_addr, pointer_buff);   //transfer addresses of strings into pointer_buff                                                       

            while(count>0){
                args[arg_num]= readVirtualMemoryString(Lib.bytesToInt(pointer_buff, arg_num*4), 256);  //DAC TO DO: make sure maxlength is appropriate(might need to be less than 256)                                                                                                                                       
                count--;
                arg_num++;
	    } 

	}
    //create a new process, make child/parent relationship, set new pid                                                                                  
    UserProcess child = UserProcess.newUserProcess();
    if(child==null){
	return -1;
    }
    boolean success=false;                                                                                                                        
    if(arg_num==0){
	success=child.execute(filename,new String[]{});  //execute child process                                                                          
    }else{
        success=child.execute(filename,args);  //execute child process                                                                                     
    }
    if(!success){

        UserKernel.numProcessLock.acquire();                       //DAC DEBUG: there is a better way to do this, 
        UserKernel.numActiveProcesses--;  //AFTER MERGE2  
        UserKernel.numProcessLock.release();
	return -1;
    }
    child.parent=this;
    this.children.add(child);

    //execute new process -hooray

   return child.pid;
}

    /*
     * handle exit()
     */
    private void handleExit(int status){         //AFTER MERGE3 
	unloadSections();
	coff.close();
        for(int i=0;i<file_descriptors.length;i++){
	    if(file_descriptors[i]!=null){
		handleClose(i);             //this should work
            }
        }
        if(children.size()!=0){
              for(int j=0; j<children.size();j++){
                children.get(j).parent=null;          //does this work for linkedLists?  DAN DEBUG 
              }
	}
        freeUserProcessBucket bucket=new freeUserProcessBucket(status,this.pid);
        if(parent!=null){
          this.parent.freeLock.acquire();       
          this.parent.freeChildren.add(bucket);   //indicate that you're free
          this.parent.freeCondition.wake();     //wake up parent if sleeping to let know that a new child has exited
          this.parent.freeLock.release();  
        }
        UserKernel.numProcessLock.acquire();
        UserKernel.numActiveProcesses--;
	//       System.out.print("PID of Process before the very end "+pid+"\n");                      //TRACE
	//        System.out.print("number of processes left:\t"+UserKernel.numActiveProcesses+"\n");   //TRACE
        if(UserKernel.numActiveProcesses==0){
	    Kernel.kernel.terminate();   //DAC DEBUG  - this should work per instructions
        }
        UserKernel.numProcessLock.release();

	UThread.finish();   //not sure about this DANDEBUG  
        System.out.print("Never Should reach here in handleExit()\n");
        return;         //never should get here
    }

    private int handleJoin(int pid, int statusAddress){
        UserProcess child=null;      
        int status=0;
        int foundFreeChild=0;
        for (int i=0;i<children.size();i++){
		if (children.get(i).pid == pid){
			child=children.get(i);
			break;
	        }
           
        }

        if (child==null){      //didn't find it, not a child
	    System.out.print("Invalid PID.  Process is not a child process\n");
           return -1;
	}
        freeLock.acquire();
        do{
           foundFreeChild=searchFreeChildren(pid);
           if(foundFreeChild==-2){
	       System.out.print("going to sleep now to wait for my kiddie\n");
              freeCondition.sleep();
           }
        }while(foundFreeChild==-2);
		System.out.print("workin' in the real world\n");
        freeLock.release();   //got the child
        this.children.remove(child);   //remove the child from the list of children so join can't be called on it again  DAC DEBUG

        status=foundFreeChild;
        if(status==-1){          //child exited with status of -1: unhandled syscall
	    return 0;
        }
        byte[] stat = new byte[4];
        Lib.bytesFromInt(stat, 0, status);
        int bytesWritten = writeVirtualMemory(statusAddress, stat);
        return 1;  
    }
   
    /*
     *searchFreeChildren  - goes through freeChildren to see if the given PID is there
     * params:
     *   int PID
     * return: status if found, -2 otherwise
     */
    private int searchFreeChildren(int pid){      //DAC DEBUG, MAKE THIS BETTER TO DO!
        freeUserProcessBucket buck;
	if(freeChildren.size()==0){          //not strictly necessary, but maybe good    //AFTER MERGE4
	    return -2;
	}else{
	    for(int i=0;i<freeChildren.size();i++){
		buck=freeChildren.get(i);
		if (buck.getPID() == pid){                        //this can only happen once if each process has a different PID and other stuff works
		    return buck.getStatus();   //got it!
		}
	    }
        }
        return -2;         //should be better than this
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	int filename_addr=0;
        int fd=0;
	int num_bytes=0;
        int buffer_address=0;
        int count=0;      //AFTER MERGE3
        int arg_address=0;

	switch (syscall) {
	case syscallHalt:
	    return handleHalt();

        case syscallCreate:
            filename_addr=a0; 
            //sanity check filename_addr
            return handleCreate(filename_addr);

	case syscallOpen:
	    filename_addr=a0;
            //sanity check filename_addr
            return handleOpen(filename_addr);
	case syscallRead:
            fd=a0;  //file descripter should be a0
            buffer_address = a1;  //buffer to read from 
            num_bytes = a2;  //number of bites to be written
            //check to see file descriptor valid
            //check to see if buffer is valid
            //ensure count is non-negative/valid
            return handleRead(fd, buffer_address, num_bytes);    //call hanlder that handles "read"  (make sure to add correct arguments)DAC             
            

        case syscallWrite:           //int write(int fileDescriptor, void *buffer, int count);
            fd=a0;  //file descripter should be a0
            buffer_address=a1;  //buffer to read from 
            num_bytes = a2;  //number of bites to be written
            //check to see file descriptor valid
            //check to see if buffer is valid
            //ensure count is non-negative/valid
            return handleWrite(fd, buffer_address, num_bytes);    //call hanlder that handles "write"  (make sure to add correct arguments)DAC  
         
        case syscallClose:
	    fd=a0;   //sanitize?
	    return handleClose(fd);
 
	case syscallUnlink:
	    fd=a0;
	    return handleUnlink(fd);
        
        case syscallExec:       //AFTER MERGE3  
	    filename_addr=a0;
            count=a1;
            arg_address=a2;
            return handleExec(filename_addr, count, arg_address);

	    case syscallExit:       //AFTER MERGE3
            int status=a0;
            handleExit(status); 
            break;

	    case syscallJoin:
            int pid=a0;
            int status_addr= a1;
            return handleJoin(pid, status_addr);
	       //AFTER MERGE3 

	default:                          //TO DO DAC DEBUG: Upon Default, launch exit with -1? 
	    handleExit(-1);
	    /*   Lib.debug(dbgProcess, "Unknown syscall " + syscall);
		 Lib.assertNotReached("Unknown system call!"); */
	}
	return 0;
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
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    	      handleExit(-1);          //DAN DEBUG
		      return;  
			       /*   	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
		      Lib.assertNotReached("Unexpected exception");  */
	}
    }

    /******************************************************************************************************
     * DAN AND PINKY ADDED - FUNCTIONS  AFTER MERGE2
     *****************************************************************************************************/
    /*
     *setPID - sets PID of this process
     */
    public void setPID(int pid){   //AFTER MERGE2 DAC added
       this.pid=pid;
       return;
     }
    /*
     *getPID - gets PID of this process
     */
    public int getPID(){   //AFTER MERGE2 DAC added
       return this.pid;
     }
  
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;

    /************************************************************************************************************************************************
     * STUFF ADDED BY DAN AND PINKY
     ***********************************************************************************************************************************************/
    private int pid;        //AFTER MERGE  FOLLOWING
    private static int global_pid=0;
    public UserProcess parent=null;
    public LinkedList<UserProcess> children = new LinkedList<UserProcess>();    //list of children of this process
    public LinkedList<freeUserProcessBucket> freeChildren = new LinkedList<freeUserProcessBucket>(); //list of free child ids + status
    //    public int freeChild=0;  //used for join(). When child finishes increments this by one and adds itself to freeChildren
    public Lock freeLock=new Lock();  //protects freeChild
    public Condition freeCondition=new Condition(freeLock);  //puts process to sleep if freeChild==false and waiting on Join
    public boolean isRoot=false;  //only true for first process
    //END AFTER MERGE


    //AFTER MERGE2
    


    //public for now: array of files associated with this process
    private final int NUM_FD = 16;    //number of files to support
    private OpenFile[] file_descriptors;

    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';


                                                                                                                                                      

    /*
    	public void testFileSystem(){
	    int flag=0;
            int read=0;
            int written=0;
            byte[] writeBuffer= new byte[4096];
            byte[] readBuffer= new byte[4096];

            OpenFile file = ThreadedKernel.fileSystem.open("blooper", true);
            if(file==null){
		System.out.print("Something wrong with fileSystem.open()\n");
            }
            flag=getNextFileDescriptor();
	    System.out.print("Flag value given by getNextFileDescriptor():\t" + flag + "\n");
            file_descriptors[flag]=file;
            if(file_descriptors[flag]==null){
		System.out.print("Something wrong with file_descriptors[flag]:\t has a null value\n");
            }else{
                for(int i=0; i<4096;i+=2){
		    writeBuffer[i]=98;
                }
                for(int i=1; i<4096;i+=2){
		    writeBuffer[i]=0;
                }
                written=file_descriptors[flag].write(writeBuffer, 0, 2000);
		//                written=file_descriptors[1].write(writeBuffer, 0, 40);   //try writing to stdout
                if(written<0){
		    System.out.print("Error writing to file\n");
                }
                System.out.print("Wrote to file," + written + " bytes of value "+writeBuffer[0]+"\n");  
		/*	System.out.print("Buffer Accessed, reading from file\n");
                //read=file_descriptors[flag].read(0, readBuffer, 0, 2000);
	                       System.out.print("Attempting to read from stdin\n");
		//read=file_descriptors[0].read(0, readBuffer, 0, 20);
		System.out.print("Read from file," + read + " bytes of value "+readBuffer[0]+"\n");  

                
		    return;    
    	    }
	    }  */

                


}

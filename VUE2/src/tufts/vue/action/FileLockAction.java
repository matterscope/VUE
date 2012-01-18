/*
 * This addition Copyright 2010-2012 Design Engineering Group, Imperial College London
 * Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
* @author  Helen Oliver, Imperial College London 
*/

package tufts.vue.action;

import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;

import tufts.vue.*;
import tufts.vue.gui.VueMenuBar;
import tufts.Util;

public class FileLockAction extends VueAction
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(FileLockAction.class);
    
    private static final String UNLOCKABLE_FILE = "NOLOCK";
    
    public FileLockAction(String label) {
        super(label, null, ":general/Lock");
    }
    
    public FileLockAction(File theFile, boolean bOpening) {
        this("Lock");
        setFile(theFile);
        setOpening(bOpening);
    }
    
    public FileLockAction() {
        this("Lock");
    }
    
    private boolean bOpening;
    private File mFile;
    
    public void setFile(File theFile) {
    	mFile = theFile;
    }
    
    public File getFile() {
    	return mFile;
    }
    
    public void setOpening(boolean opening) {
    	bOpening = opening;
    }
    
    public boolean isOpening() {
    	return bOpening;
    }
    
    private void init() {
    	LWMap activeMap = VUE.getMapInActiveTab();
    	if (activeMap != null)
    		setFile(activeMap.getFile());
    	setOpening(false);
    }
    
    private static final Object LOCK = new Object();
    private static boolean lockUnderway = false;
    public void actionPerformed(ActionEvent e) {
        synchronized (LOCK) {
            if (lockUnderway)
                return;
            lockUnderway = true;
        }
        try {
        	init();
        	
        	createLockFile(mFile, bOpening, true);      
        	
            Log.info(e.getActionCommand() + ": completed.");
        } finally {
            lockUnderway = false;
            
            
        }
    }  
    
    
    
    // HO 04/01/2012 BEGIN *********
    /**
     * Function to determine whether a file is writable by the current
     * user. It's writable if a) it hasn't been marked read-only and
     * b) it isn't locked by any other user.
     * @param theFile, the File object which may or may not be writable.
     * @return the original input param file if the file is just not writable,
     * null if the file is writable and not locked by any other user,
     * the lock File if the file is locked by another user.
     * @author Helen Oliver
     */
    public static File isFileWritableByCurrentUser(File theFile) {
    	File lockFile = null;
    	
    	// input validation
    	if (theFile == null) {
    		return lockFile;
    	}
    	
    	// if for whatever reason the file has been set not-writable,
    	// return the original file
    	boolean bWritable = theFile.canWrite();
    	if (bWritable == false)
    		return theFile;
    	
    	// check and see if the file is locked by another user
    	lockFile = isFileLockedByOtherUser(theFile);
    	
    	return lockFile;
    }
    
    /**
     * Function to determine whether a file is locked by the current user.
     * It's locked by the current user if there is a file of type
     * ".vlk" in the same folder, with the same name, prefixed
     * with the current user name.
     * @param theFile, the file which may or may not be locked by the current user.
     * @return true if the file is locked by the current user,
     * false otherwise.
     * @author Helen Oliver
     */
    public static File isFileLockedByCurrentUser(File theFile) {
    	File lockFile = null;
    	
    	// input validation
    	if (theFile == null)
    		return lockFile;
    	
    	// directory to look in
    	File targetDir = theFile.getParentFile();
    	// get the name to match against
    	String strMatchingName = lockFilePrefix(theFile);
    	if (targetDir.isDirectory()) {
    		// get all the VUE lock files in the directory
			File[] dir = targetDir.listFiles(appropriateFilter(theFile));	
			
			// if we have a list of files, cycle through them
			if (dir != null) {	
				for (int i = 0; i < dir.length; i++) {
					// get the next file in the directory
					File file_test = dir[i];
					String strNextFilename = file_test.getName().toString();
						
						if (strNextFilename.equals(strMatchingName + lockFileSuffix(theFile.getName()))) {
								String strUserName = userWhoHasLockedAFile(file_test);
								if ((strUserName != "") && (strUserName.equals(System.getProperty("user.name")))) {
									lockFile = file_test;
									break;
								}
						}
				}
			}
    	}
    	
		return lockFile;

    }
    
    /**
     * A function to open the lock file, read a line from it,
     * and return the line. The line will be the name
     * of the user that has the lock on the file.
     * @param lockFile, the lock file
     * @return an empty string if there's nothing in the lock file
     * or something goes wrong in the reading of it;
     * or the username of the user who has the lock if there is a line in there
     * @author Helen Oliver
     */
    public static String userWhoHasLockedAFile(File lockFile) {
    	// the user name-to-be
    	String strUserWithLock = "";
    	
    	// open the file and read a line from it
    	try {
	    	BufferedReader br = new BufferedReader(new FileReader(lockFile));
	    	strUserWithLock = br.readLine();
	    	br.close();
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
        
    	// return what we read from the file
    	return strUserWithLock;
    }
    

    
    /**
     * Function to determine whether a file is locked by another user.
     * A file is locked by another user if there is a file of
     * the same name in the same folder, with the extension ".vlk",
     * and prefixed by not-the-current-user's name.
     * @param theFile, the File that may or may not be locked by another user.
     * @return the lock File belonging to the other user, if there is one
     * @author Helen Oliver
     */
    public static File isFileLockedByOtherUser(File theFile) {
    	File lockFile = null;
    	
    	// input validation
    	if (theFile == null)
    		return lockFile;
    	
    	// directory to look in
    	File targetDir = theFile.getParentFile();

		String strMatchingName = lockFilePrefix(theFile);
		// get the username to match against
		String strMatchingUsername = System.getProperty("user.name");
		// now go through the directory
    	if (targetDir.isDirectory()) {
    		// get all the files in the directory
			File[] dir = targetDir.listFiles(appropriateFilter(theFile));	
			
			// if we have a list of files, cycle through them
			if (dir != null) {	
				for (int i = 0; i < dir.length; i++) {
					// get the next file in the directory
					File file_test = dir[i];
					String strNextFilename = file_test.getName().toString();
						if (strNextFilename.equals(strMatchingName + lockFileSuffix(theFile.getName()))) {
								String strUserName = userWhoHasLockedAFile(file_test);
								if ((strUserName != "") && (!strUserName.equals(System.getProperty("user.name")))) {
									lockFile = file_test;
									break;
								}
						}
				}
			}
    	}
    	
    	return lockFile;
    }
    
    
    /**
     * A function to determine what the lock file prefix should be
     * for the current user.
     * @param theFile, the File object for which to determine the prefix.
     * @return a String representing the lock file prefix
     * @author Helen Oliver
     */
    private static String lockFilePrefix(File theFile) {
    	// input validation
    	if (theFile == null)
    		return null;

		// and use the name of the file being locked too
		String strLockFileName = theFile.getName();
		
		// strip off the existing suffix
		strLockFileName = strLockFileName.substring(0, strLockFileName.length() - 4);
		
		return strLockFileName;
    }
    
    /**
     * A function to return the suffix for the VUE lock file type.
     * @return a String representing the suffix for the VUE lock file type,
     * or a constant 
     * @author Helen Oliver
     */
    private static String lockFileSuffix(String strFileName) {
    	String strLockFileSuffix = UNLOCKABLE_FILE;
    	
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		strLockFileSuffix = VueUtil.VueLockExtension;
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		strLockFileSuffix = VueUtil.VueArchiveLockExtension;

    	return strLockFileSuffix;
    }
    
    /**
     * A method to delete what is probably the previous
     * lock file, when saving to a new file.
     * @param map, the LWMap which may or may not have a File that needs to be released
     * from any existing locks.
     * @param newFile, the new file-to-be
     * @author Helen Oliver
     */
    public static void deletePreviousLockFile(LWMap map, File newFile) {
    	// input validation
    	if (map == null)
    		return;
    	
    	String newName = newFile.getName();
    	String newPath = newFile.getParentFile().toString();
    	
    	File mapFile = map.getFile();
    	
    	if (mapFile != null) {
    		String oldName = mapFile.getName();
        	String oldPath = mapFile.getParentFile().toString();
        	
    		// if they have different names
    		if (!oldName.toLowerCase().equals(newName.toLowerCase())) {
    			deleteLockFile(mapFile);
    		} else if (!oldPath.toLowerCase().equals(newPath.toLowerCase())) {
    			// if they have the same name but are on different paths
    			deleteLockFile(mapFile);    			
    		}
    	}
    }
    // HO 04/01/2012 END **********
    
 // HO 21/12/2011 BEGIN ********
    /**
     * A method to create a lock file.
     * The lock file is in the same directory as the
     * file being locked.
     * The file can't be locked if it's not writable
     * in the first place, or is already locked
     * by someone else. Otherwise it will be
     * locked by the current user.
     * @param theFile, the File object to be locked.
     * @param bOpening, true if we are in the process of opening a file,
     * false otherwise.
     * @param bNotifying, true if we are notifying the user that the file has been locked,
     * false otherwise.
     * @author Helen Oliver
     */
    public static void createLockFile(File theFile, boolean bOpening, boolean bNotifying) {
   
    	if (theFile != null) {
    		try {
    				File lockFile = isFileLockedByCurrentUser(theFile);
	    			if (lockFile != null) {
	    				// if the current user already has
	    				// a lock on the file, do nothing
	    				// except making sure it will delete when
	    				// the VM does
	    				lockFile.deleteOnExit();
	    				return;
	    			} else {
	    				// if the file is writable at all
	    				boolean bWritable = checkIfFileIsWritable(theFile, bOpening, bNotifying);
	    			
	    				// if the file is writable for this user
	    				if (bWritable) {
		    				// create a temporary file so the prefix is the current user name
	    					String strLockFileName = lockFilePrefix(theFile);
		    				String strLockFilePrefix = strLockFileName;
		    				// get appropriate suffix for lock file suffix
		    				String strLockFileSuffix = lockFileSuffix(theFile.getName());
		    				// if the file is of a type that can't be locked,
		    				// notify the user and return 
		    				if (strLockFileSuffix.equals(UNLOCKABLE_FILE)) {
		    					// only show this if we are notifying the user
		    					if (bNotifying) {
			    					JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
			    			                strLockFileName + " cannot be locked.\n"
			    			                + "Only files of type .VUE and .VPK can be locked.",
			    			                "File cannot be locked.", 
			    			                JOptionPane.WARNING_MESSAGE);
		    					}
		    					
		    					return;
		    				}
		    					
		    				// and the directory is the same one as the file is in
		    				File lockFileDirectory = theFile.getParentFile();
		    				// now create the lock file in the right directory
		    				try {
		    					// create the lock file
		    					//lockFile = File.createTempFile(strLockFilePrefix, strLockFileSuffix, lockFileDirectory);
		    					lockFile = new File(lockFileDirectory, strLockFilePrefix + strLockFileSuffix);
		    					// write the username to the lock file
		    					BufferedWriter bw = new BufferedWriter(new FileWriter(lockFile));
		    		    	    String strCurrentUser = System.getProperty("user.name");
		    		    	    String strFileName = theFile.getName();
		    					bw.write(strCurrentUser);
		    		    	    bw.close();
		    					// make sure the lock file gets deleted when the virtual machine terminates
		    					lockFile.deleteOnExit();
		    					if ((bNotifying) && (!bOpening)) {
		    						JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
		    				             "You have locked the file " + strFileName + ".",
		    			                "File locked by " + strCurrentUser, 
		    			                JOptionPane.INFORMATION_MESSAGE);
		    					}
		    				} catch (IOException e) {
		    					e.printStackTrace();
		    				}
	    				} else {
	    					
	    				}
	    			} 
    		} catch (SecurityException se) {
    			se.printStackTrace();
    		}
    	} else { // there's no file object
    		if ((bNotifying) && (!bOpening)) { // if we are notifying the user of the lock's success or failure, 
    			// and we are not in the process of opening another file, show message
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
    	                "There is no file to lock.",
                    "No file object", 
                    JOptionPane.WARNING_MESSAGE);
    		}
    	}
    	
    }
    // HO 21/12/2011 END ****** 
    
    
    // HO 05/01/2012 BEGIN ************    

    
    /**
     * A method to find the current user's lock file
     * on the given File object, if there is one, and
     * delete it
     * @param theFile, the File object for which to find and
     * delete the current user's lock file, if there is one
     */
    public static void deleteLockFile(File theFile) {
    	// routine to figure out if there's a lock file,
    	// and delete it if there is
    	File lockFile = isFileLockedByCurrentUser(theFile);
    	if (lockFile != null) {
    		lockFile.delete();    		
    	}
    }
    
    /**
     * A method to find the current user's lock file
     * on the given File object, if there is one, and
     * delete it
     * @param theFile, the File object for which to find and
     * delete the current user's lock file, if there is one
     * @param bShowingMessage, true if we are showing the user a message
     * @author Helen Oliver
     */
    public static void deleteLockFile(File theFile, boolean bShowingMessage) {
    	// routine to figure out if there's a lock file,
    	// and delete it if there is
    	File lockFile = isFileLockedByCurrentUser(theFile);
    	if (lockFile != null) {
    		lockFile.delete();    	
			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
	                "File unlocked.\n"
                	+ "Your changes may be overwritten by other users.",
                "File unlocked", 
                JOptionPane.WARNING_MESSAGE);
    	} else {
    		lockFile = isFileLockedByOtherUser(theFile);
    		if (lockFile != null) {
    			String strUserWithLock = userWhoHasLockedAFile(lockFile);
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
	                strUserWithLock + " has locked this file for writing.\n"
                	+ "You cannot unlock this file.",
                "File locked by other user", 
                JOptionPane.WARNING_MESSAGE);
    		} else {
    			JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
    	                "There is no file to unlock.",
                    "No file object", 
                    JOptionPane.WARNING_MESSAGE);
    		}
    	}
    }    
    
    // HO 21/12/2011 BEGIN ******
    /**
     * A function to check whether the file is writable
     * @param file, the file to check
     * @param bOpening, true if we are in the process of opening a file, false if not
     * @param bNotifying, true if we are notifying the user of the status, false if not
     */
    public static boolean checkIfFileIsWritable(File file, boolean bOpening, boolean bNotifying) {
    	File lockFile = isFileWritableByCurrentUser(file);
    	
	    // if it's writable, we either get back null or a
    	// File object that is NOT the same one we sent in
    	if (lockFile == null) {
	    	System.out.println("yes it's writable");
	    	return true;
	    } else if (lockFile.equals(file)) {
	    	if (bNotifying) {
		    	JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
		                "That file is not writable.", 
		                "Can't save", 
		                JOptionPane.ERROR_MESSAGE);
	    	}
	    	return false;
	    } else {
	    	if (!bOpening) {
	    		if (bNotifying) {
			    	notifyThatFileIsLocked(file, lockFile);
	    		}
	    	} else {
	    		if (bNotifying) {
	    			notifyThatFileIsLocked(file, lockFile);
	    		}
	    	}
	    	return false;
	    }
    }
    // HO 21/12/2011 END ********
    // HO 05/01/2012 END **************    
    
    /**
     * A function to return the right kind of file extension
     * filter for a given file.
     * @param theFile, the file for which we need the right extension filter
     * @return either a VueLockFileFilter or a VueArchiveLockFileFilter,
     * according to the file type
     * @author Helen Oliver
     */
    private static FileFilter appropriateFilter(File theFile) {
    	if (theFile == null)
    		return null;
    	
    	String strFileName = theFile.getName();
    	if (strFileName.endsWith(VueUtil.VueExtension))
    		return new VueLockFileFilter();
    	
    	else if (strFileName.endsWith(VueUtil.VueArchiveExtension))
    		return new VueArchiveLockFileFilter();
    	
    	return null;
    	
    }
    
    static class VueLockFileFilter implements FileFilter {

  	  public boolean accept(File pathname) {

  	    if (pathname.getName().endsWith(VueUtil.VueLockExtension)) 
  	      return true;
  	    return false;
  	  }
  	}
    
    // HO 18/01/2012 BEGIN *******
    /**
     * A method to show a notification that a given file is locked,
     * and by whom.
     * @param file, the File that is locked
     * @param lockFile, the lock file in question
     * @author Helen Oliver
     */
    public static void notifyThatFileIsLocked(File file, File lockFile) {
    	String strUserWithLock = userWhoHasLockedAFile(lockFile);
    	
    	JOptionPane.showMessageDialog((Component)VUE.getApplicationFrame(),
                strUserWithLock + " has locked the file\n"
            	+ file.getName() + "\nfor writing.\n"
            	+ "Your changes will not be saved.",
            "File locked by other user.", 
            JOptionPane.ERROR_MESSAGE);
    }
    // HO 18/01/2012 END *********
    
    static class VueArchiveLockFileFilter implements FileFilter {

    	  public boolean accept(File pathname) {

    	    if (pathname.getName().endsWith(VueUtil.VueArchiveLockExtension))
    	      return true;
    	    return false;
    	  }
    	}
    
}
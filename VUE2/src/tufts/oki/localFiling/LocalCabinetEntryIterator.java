/*
 * CabinetEntryIterator.java
 *
 * Created on September 17, 2003, 1:05 PM
 *
 *  The software contained in this file is �2003 by Mark J. Norton, all rights reserved.
 */

package tufts.oki.localFiling;
import java.util.*;

/**
 *  The CabinetEntryIterator provides a way to list all entries in a given cabinet entry.
 *
 * @author  Mark Norton
 *
 *  CabinetEntryIterator is fully implemented.
 */
public class LocalCabinetEntryIterator implements osid.filing.CabinetEntryIterator{
    
    private Vector cabinet_vector = null;
    
    private int offset = 0;
    
    /** Creates a new instance of CabinetEntryIterator  given a vector of CabinetEntry.  */
    public LocalCabinetEntryIterator(Vector vect) {

        cabinet_vector = vect;
    }
    
    /**  Check to see if there is at least one more entry.  */
    public boolean hasNext() throws osid.filing.FilingException {
        return (offset < cabinet_vector.size());
    }
    
    /**  Get the next entry and increment offset.  */
    public osid.filing.CabinetEntry next() throws osid.filing.FilingException {
        osid.filing.CabinetEntry ce = (osid.filing.CabinetEntry) cabinet_vector.elementAt(offset);
        offset++;
        return ce;
    }
    
}

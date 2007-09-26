/**
 * 
 */
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

class UntrackedFilesFilter extends ViewerFilter 
  {
    public UntrackedFilesFilter() 
    {
      super();
    }

    /**
     * Filter out untracked files.
     */
    public boolean select(Viewer viewer, Object parentElement,Object element) 
    {
      if (element instanceof CommitResource) 
      {
        String str = ((CommitResource) element).getStatus();
        return str.startsWith(CommitDialog.FILE_UNTRACKED) != true;
      }
      return true;
    }
  }
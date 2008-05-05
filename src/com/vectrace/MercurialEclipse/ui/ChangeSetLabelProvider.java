package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.vectrace.MercurialEclipse.model.ChangeSet;

public class ChangeSetLabelProvider
        extends LabelProvider
        implements ITableLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }

    public String getColumnText(Object element, int columnIndex) {
        ChangeSet rev = (ChangeSet) element;
        switch(columnIndex) {
            case 0:
                return Integer.toString(rev.getChangesetIndex());
            case 1:
                return rev.getChangeset();
            case 2:
                return rev.getDate();
            case 3:
                return rev.getUser();
        }
        return null;
    }
}

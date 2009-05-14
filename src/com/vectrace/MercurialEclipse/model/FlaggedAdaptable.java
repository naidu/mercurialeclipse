package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IAdaptable;

public class FlaggedAdaptable implements IAdaptable {

    private final IAdaptable adaptable;
    private final char flag;
    

    public FlaggedAdaptable(IAdaptable adaptable, char flag) {
        this.adaptable = adaptable;
        this.flag = flag;
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        return adaptable.getAdapter(adapter);
    }

    public char getFlag() {
        return this.flag;
    }
    
    public String getStatus() {
        return flag == 'U' ? Messages.getString("FlaggedAdaptable.unresolvedStatus") : Messages.getString("FlaggedAdaptable.resolvedStatus");
    }
}

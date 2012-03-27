package com.vectrace.MercurialEclipse.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.text.Match;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;

public class MercurialMatch extends Match {

	private ChangeSet changeset;
	private int lineNumber;
	private String extract;
	private HgFile hgFile;
	private Region originalLocation;
	private final IFile file;
	private boolean becomesMatch;

	/**
	 * @param matchRequestor
	 */
	public MercurialMatch(MercurialTextSearchMatchAccess ma) {

		super(ma.getHgFile(), -1, -1);
		this.changeset = ma.getRev();
		this.lineNumber = ma.getLineNumber();
		this.extract = ma.getExtract();
		this.hgFile = ma.getHgFile();
		this.becomesMatch = ma.isBecomesMatch();
		this.file = ma.getFile();
	}

	/**
	 * @param file
	 */
	public MercurialMatch(IFile file) {
		super(file, -1, -1);
		this.file = file;
	}

	@Override
	public void setOffset(int offset) {
		if (originalLocation == null) {
			// remember the original location before changing it
			originalLocation = new Region(getOffset(), getLength());
		}
		super.setOffset(offset);
	}

	@Override
	public void setLength(int length) {
		if (originalLocation == null) {
			// remember the original location before changing it
			originalLocation = new Region(getOffset(), getLength());
		}
		super.setLength(length);
	}

	public int getOriginalOffset() {
		if (originalLocation != null) {
			return originalLocation.getOffset();
		}
		return getOffset();
	}

	public int getOriginalLength() {
		if (originalLocation != null) {
			return originalLocation.getLength();
		}
		return getLength();
	}

	public IFile getFile() {
		return file;
	}

	public boolean isFileSearch() {
		return lineNumber == 0;
	}

	public Region getOriginalLocation() {
		return originalLocation;
	}

	public void setOriginalLocation(Region originalLocation) {
		this.originalLocation = originalLocation;
	}

	public ChangeSet getChangeSet() {
		return changeset;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getExtract() {
		return extract;
	}

	public HgFile getHgFile() {
		return hgFile;
	}

	@Override
	public int getOffset() {
		// TODO Auto-generated method stub
		return super.getOffset();
	}

	/**
	 * @return the becomesMatch
	 */
	public boolean isBecomesMatch() {
		return becomesMatch;
	}
}

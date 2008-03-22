package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class IgnoreDialog extends Dialog {

	private enum Type {
		FILE, FOLDER, NONE
	}
	
	public enum ResultType {
		FILE, FOLDER, EXTENSION, GLOB, REGEXP
	}
	
	private Type type;
	private ResultType resultType;
	private IFile file;
	private IFolder folder;
	
	Text patternText;
	private String pattern;

	public IgnoreDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.type = Type.NONE;
	}

	public IgnoreDialog(Shell parentShell, IFile file) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.type = Type.FILE;
		this.file = file;
	}

	public IgnoreDialog(Shell parentShell, IFolder folder) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.type = Type.FOLDER;
		this.folder = folder;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Add to hgignore...");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout(1, true);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Select what to ignore:");

		switch(type) {
		case FILE:
			addButton(composite, "Only this file", false, ResultType.FILE);
			addButton(composite, "All files with the same extension", false, ResultType.EXTENSION);
			break;
		case FOLDER:
			addButton(composite, "Only this folder", false, ResultType.FOLDER);
		}
		addButton(composite, "Custom regexp", true, ResultType.REGEXP);
		addButton(composite, "Custom glob", true, ResultType.GLOB);

		patternText = new Text(composite, SWT.BORDER | SWT.DROP_DOWN);
		patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return composite;
	}
	
	private void addButton(Composite parent, String text, final boolean isPattern, final ResultType type) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(text);
		button.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				patternText.setEnabled(isPattern);
				resultType = type;
			}
		});
	}

	@Override
	protected void okPressed() {
		this.pattern = patternText.getText();
		super.okPressed();
	}

	public IFile getFile() {
		return file;
	}

	public IFolder getFolder() {
		return folder;
	}

	public String getPattern() {
		return pattern;
	}

	public ResultType getResultType() {
		return resultType;
	}

}

package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class RevisionChooserDialog extends Dialog {

	private final String title;
	private Combo combo;
	private String revision;
	private String[] revisions;
	
	public RevisionChooserDialog(Shell parentShell, String title, String[] revisions) {
		super(parentShell);
		this.title = title;
		this.revisions = revisions;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
	      Composite composite = (Composite)super.createDialogArea(parent);
	      composite.setLayout(new FillLayout(SWT.VERTICAL));
	      Label label = new Label(composite, SWT.NONE);
	      label.setText("Please enter a valid revision (local, global or tag):");
	      combo = new Combo(composite, SWT.BORDER | SWT.DROP_DOWN);
	      combo.setItems(revisions);
	      return composite;
	}

	@Override
	protected void okPressed() {
		revision = combo.getText().split(":")[0].trim();
		if(revision.length()==0) {
			revision = null;
		}
		super.okPressed();
	}

	public String getRevision() {
		return revision;
	}
}

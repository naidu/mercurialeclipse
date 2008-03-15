package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/* Very simple dialog for the time being, but it does its job 
 * TODO let the user pick from a list of valid revisions/tags
 */
/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class RevisionChooserDialog extends Dialog {

	private final String title;
	private Text text;
	private String revision;
	
	public RevisionChooserDialog(Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
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
	      text = new Text(composite, SWT.BORDER);
	      return composite;
	}

	@Override
	protected void okPressed() {
		revision = text.getText().trim();
		if(revision.length()==0) {
			revision = null;
		}
		super.okPressed();
	}

	public String getRevision() {
		return revision;
	}
}

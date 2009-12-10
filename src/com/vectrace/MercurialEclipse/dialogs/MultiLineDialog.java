package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MultiLineDialog extends Dialog {

	private final String title;
	private final String message;

	public MultiLineDialog(Shell parentShell, String title, String message) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.title = title;
		this.message = message;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		  Composite composite = (Composite)super.createDialogArea(parent);
		  composite.setLayout(new FillLayout());
		  Text text = new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		  text.setFont(JFaceResources.getTextFont());
		  text.setBackground(getParentShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		  text.setText(message);
		  return composite;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
}

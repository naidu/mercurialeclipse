/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.operations.CloneOperation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 * @author Andrei Loskutov
 */
public class ClonePage extends PushPullPage {

	private Button pullCheckBox;
	private Button uncompressedCheckBox;
	private Text revisionTextField;
	private Button goButton;
	private final Map<HgRepositoryLocation, File> destDirectories = new HashMap<HgRepositoryLocation, File>();
	private HgRepositoryLocation lastRepo;
	private Text directoryTextField;
	private Button directoryButton;
	private Button useWorkspace;
	private Text cloneNameTextField;
	static private File lastUsedDir;

	public ClonePage(IResource resource, String pageName, String title,
			ImageDescriptor titleImage) {
		super(resource, pageName, title, titleImage);
		this.resource = resource;
		setShowBundleButton(false);
		setShowRevisionTable(false);
		setShowCredentials(true);
		setShowForce(false);
		setShowSnapFile(false);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();
		createDestGroup(composite);
		createOptionsGroup(composite);
		createBigButton(composite);
		setPageComplete(false);
	}

	private boolean isEmpty(String s){
		return s == null || s.trim().length() == 0;
	}

	private void createBigButton(Composite composite) {
		Composite panel = new Composite(composite, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		panel.setLayout(new GridLayout());
		goButton = new Button(panel, SWT.CENTER);
		goButton.setText("Clone data from repository");
		goButton.setFont(JFaceResources.getBannerFont());
		goButton.setEnabled(false);
		goButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean ok = validateFields();
				if(!ok) {
					HgRepositoryLocation repository = getRepository();
					if (hasDataLocally(repository)
							&& getDestinationDirectory().equals(destDirectories.get(repository))) {
						return;
					}
				}
				setErrorMessage(null);
				boolean dataFetched = finish(null);
				setPageComplete(dataFetched);
				goButton.setEnabled(!dataFetched);
				if(dataFetched){
					// simply switch to the next page
					getWizard().getContainer().showPage(getNextPage());
				}
			}
		});
	}

	private void createDestGroup(Composite composite) {
		Group g = SWTWidgetHelper.createGroup(composite, Messages
				.getString("ClonePage.destinationGroup.title"), 3, GridData.FILL_HORIZONTAL); //$NON-NLS-1$

		useWorkspace = SWTWidgetHelper.createCheckBox(g, "Use workspace as parent directory");
		GridData layoutData = new GridData();
		layoutData.horizontalSpan = 3;
		useWorkspace.setLayoutData(layoutData);
		final String wsRootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		useWorkspace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(useWorkspace.getSelection()){
					directoryTextField.setText(wsRootLocation);
					directoryTextField.setEnabled(false);
					directoryButton.setEnabled(false);
				} else {
					directoryTextField.setText("");
					directoryTextField.setEnabled(true);
					directoryButton.setEnabled(true);
				}
			}
		});

		SWTWidgetHelper.createLabel(g, Messages
				.getString("ClonePage.destinationDirectoryLabel.title")); //$NON-NLS-1$
		directoryTextField = SWTWidgetHelper.createTextField(g);
		if(lastUsedDir != null){
			directoryTextField.setText(lastUsedDir.getAbsolutePath());
		} else {
			directoryTextField.setText(wsRootLocation);
		}
		directoryTextField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateFields();
			}
		});
		directoryButton = SWTWidgetHelper.createPushButton(g, Messages
				.getString("ClonePage.directoryButton.title"), 1); //$NON-NLS-1$
		directoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				if(!isEmpty(getRootName())) {
					dialog.setFilterPath(getRootName());
				}
				dialog.setMessage(Messages.getString("ClonePage.directoryDialog.message")); //$NON-NLS-1$
				String dir = dialog.open();
				if (dir != null) {
					directoryTextField.setText(dir);
				}
				validateFields();
			}
		});

		if(lastUsedDir == null || wsRootLocation.equals(lastUsedDir.getAbsolutePath())){
			useWorkspace.setSelection(true);
			directoryTextField.setEnabled(false);
			directoryButton.setEnabled(false);
		}

		SWTWidgetHelper.createLabel(g, Messages.getString("ClonePage.cloneDirectoryLabel.title")); //$NON-NLS-1$
		cloneNameTextField = SWTWidgetHelper.createTextField(g);
		cloneNameTextField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateFields();
				HgRepositoryLocation repo = getRepository();
				if(repo != null && hasDataLocally(repo)){
					// if only the target directory name is changed, simply rename it on
					// the disk to avoid another remote clone operation
					File file = destDirectories.get(repo);
					String cloneName = getCloneName();
					if(cloneName.length() > 0 && !file.getName().equals(cloneName)){
						File destinationDirectory = getDestinationDirectory();
						if(!destinationDirectory.exists()){
							if(file.renameTo(destinationDirectory)) {
								destDirectories.put(repo, destinationDirectory);
							}
						}
						validateFields();
					}
				}
			}
		});

		g.moveAbove(optionGroup);
	}

	private void createOptionsGroup(Composite composite) {
		Group g = optionGroup;

		pullCheckBox = SWTWidgetHelper.createCheckBox(g, Messages
				.getString("ClonePage.pullCheckBox.title")); //$NON-NLS-1$
		uncompressedCheckBox = SWTWidgetHelper.createCheckBox(g, Messages
				.getString("ClonePage.uncompressedCheckBox.title")); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, Messages
				.getString("ClonePage.revisionLabel.title")); //$NON-NLS-1$
		revisionTextField = SWTWidgetHelper.createTextField(g);
	}

	private String getCloneName() {
		return cloneNameTextField.getText();
	}

	private String getRootName() {
		return directoryTextField.getText();
	}

	public File getDestinationDirectory() {
		String root = getRootName();
		File dest = new File(root, getCloneName());
		return dest;
	}


	@Override
	protected boolean urlChanged() {
		boolean dataFetched = false;
		HgRepositoryLocation repository = null;
		if(super.validateFields()){
			repository = getRepository();
			dataFetched = hasDataLocally(repository);
		}
		if(!dataFetched) {
			cloneNameTextField.setText(guessProjectName(getUrlText()));
		}
		if(dataFetched){
			File fullPath = destDirectories.get(repository);
			directoryTextField.setText(fullPath.getParentFile().getAbsolutePath());
			cloneNameTextField.setText(fullPath.getName());
		}
		return super.urlChanged();
	}

	/**
	 * Tries to guess project name from given directory/repository url
	 * @param urlText non null
	 * @return never return null, may return empty string
	 */
	private String guessProjectName(String urlText) {
		if(urlText.length() == 0){
			return urlText;
		}
		urlText = urlText.replace('\\', '/');
		while(urlText.endsWith("/")){
			urlText = urlText.substring(0, urlText.length() - 1);
		}
		// extract last part of path/url (if any)
		int last = urlText.lastIndexOf('/');
		String guess = "";
		if(last > 0 && last + 1 < urlText.length()){
			guess = urlText.substring(last + 1);
			// many projects repositories ends with "hg", so try to skip this
			if("hg".equals(guess)){
				// google project names are the first part of the repo url, like
				// https://anyedittools.googlecode.com/hg/
				if(urlText.contains(".googlecode.")){
					return guessProjectName(urlText.substring(0, urlText.indexOf(".googlecode.")));
				}
				// just try the first part before "hg"
				return guessProjectName(urlText.substring(0, last));
			}
		}
		return guess;
	}

	private boolean hasDataLocally(HgRepositoryLocation repo){
		if(repo == null){
			return false;
		}
		File localClone = destDirectories.get(repo);
		return localClone != null && localClone.exists();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
	}

	public void performCleanup(){
		Collection<File> values = destDirectories.values();
		for (File file : values) {
			ResourceUtils.delete(file, true);
		}
	}

	@Override
	protected boolean validateFields() {
		if(goButton != null) {
			goButton.setEnabled(false);
		}
		boolean ok = super.validateFields();
		if(!ok){
			setMessage(null, INFORMATION);
			setPageComplete(false);
			return false;
		}
		boolean destinationNameOk = validateDestinationName();
		if(!destinationNameOk) {
			setPageComplete(false);
			return false;
		}
		HgRepositoryLocation repository = getRepository();
		if(repository == null) {
			setErrorMessage("Clone repository URL is invalid!");
			setPageComplete(false);
			return false;
		}
		File destinationDirectory = getDestinationDirectory();
		boolean dataFetched = destinationDirectory.equals(destDirectories.get(repository));
		if(destinationDirectory.exists() && !dataFetched){
			setErrorMessage("Directory '" + destinationDirectory + "' already exists. Please " +
			"choose a new, not existing directory!");
			setPageComplete(false);
			return false;
		} else if (!destinationDirectory.getParentFile().canWrite()) {
			setErrorMessage("You have no permissions to write into '"
					+ destinationDirectory.getParent() + "'!");
			setPageComplete(false);
			return false;
		} else {
			setPageComplete(true);
		}

		setErrorMessage(null);
		setMessage(null, INFORMATION);

		if(!dataFetched){
			setMessage("Ready to start clone", INFORMATION);
		} else {
			setMessage(null);
		}
		if(goButton != null) {
			goButton.setEnabled(!dataFetched);
		}
		setPageComplete(dataFetched);
		return dataFetched;
	}

	private boolean validateDestinationName() {
		boolean rootOk = !isEmpty(getRootName()) && new File(getRootName()).isDirectory();
		boolean nameOk = !isEmpty(getCloneName());
		if(!rootOk){
			setErrorMessage("Please specify existing parent directory for clone");
		} else if(!nameOk){
			setErrorMessage("Please specify the directory name for clone repository");
		} else {
			setErrorMessage(null);
		}
		return rootOk && nameOk;
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	@Override
	public IWizardPage getNextPage() {
		IWizardPage nextPage = super.getNextPage();
		File destinationDirectory = getDestinationDirectory();
		rememberLastUsed(destinationDirectory);
		return nextPage;
	}

	private void rememberLastUsed(File destinationDirectory) {
		lastUsedDir = destinationDirectory.getParentFile();
	}

	@Override
	protected String getTimeoutCheckBoxLabel() {
		return Messages.getString("ClonePage.timeoutCheckBox.title");
	}


	@Override
	public boolean finish(IProgressMonitor monitor) {
		boolean ok = super.finish(monitor);
		if(!ok){
			return false;
		}
		boolean forest = false;
		if (isShowForest()) {
			forest = getForestCheckBox().getSelection();
		}
		boolean svn = false;
		if (isShowSvn()) {
			svn = getSvnCheckBox().getSelection();
		}
		lastRepo = getRepository();
		if(lastRepo == null){
			setErrorMessage("Clone repository URL is invalid!");
			setPageComplete(false);
			return false;
		}

		File destDirectory = getDestinationDirectory();
		ok = destDirectory.mkdirs();
		if(!ok){
			setErrorMessage("Failed to create destination directory '" + destDirectory + "'!");
			return false;
		}
		try {
			// run clone
			boolean pull = pullCheckBox.getSelection();
			boolean uncompressed = uncompressedCheckBox.getSelection();
			boolean timeout2 = getTimeoutCheckBox().getSelection();
			String rev = revisionTextField.getText();

			CloneOperation cloneOperation = new CloneOperation(getContainer(), destDirectory
					.getParentFile(), lastRepo, false, pull, uncompressed, timeout2, rev,
					destDirectory.getName(), forest, svn);

			getContainer().run(true, true, cloneOperation);
		} catch (InvocationTargetException e) {
			ResourceUtils.delete(destDirectory, true);
			return handle(e);
		} catch (InterruptedException e) {
			// operation cancelled by user
			ResourceUtils.delete(destDirectory, true);
			return false;
		}
		destDirectories.put(lastRepo, destDirectory);
		return true;
	}

	public HgRepositoryLocation getLastUsedRepository(){
		return lastRepo;
	}

	private HgRepositoryLocation getRepository() {
		if(getUrlText().length() == 0){
			return null;
		}
		Properties props = createProperties();
		try {
			return MercurialEclipsePlugin.getRepoManager().getRepoLocation(props);
		} catch (HgException e) {
			return null;
		}
	}

	private boolean handle(Exception e) {
		if (e.getCause() != null) {
			setErrorMessage(e.getCause().getLocalizedMessage());
		} else {
			setErrorMessage(e.getLocalizedMessage());
		}
		MercurialEclipsePlugin.logError(Messages
				.getString("CloneRepoWizard.cloneOperationFailed"), e);
		return false;
	}

}

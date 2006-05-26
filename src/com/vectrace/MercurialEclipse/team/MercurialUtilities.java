/**
 * com.vectrace.MercurialEclipse (c) Vectrace Feb 3, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author zingo
 * 
 */
public class MercurialUtilities {
	static IOConsole console;

	static IOConsoleInputStream console_in;

	static IOConsoleOutputStream console_out;

	static PrintStream console_out_printstream;

	/**
	 * This class is full of utilities metods, useful allover the place
	 */
	public MercurialUtilities() 
  {

	}

	/**
	 * 
	 * @return
	 */
	public static boolean isExecutableConfigured() {
		try 
    {
			Runtime.getRuntime().exec(getHGExecutable());
			return true;
		}
    catch (IOException e) 
    {
			return false;
		}
	}

	/**
	 * Returns the executable for hg.
	 * If it's not defined, false is returned
	 * @return false if no hg is defined. True if hg executable is defined
	 */
	public static String getHGExecutable() 
  {
		IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
				.getPreferenceStore();

		// This returns "" if not defined
		String executable = preferenceStore.getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);

		return executable;
	}

	public static String getHGExecutable(boolean configureIfMissing) 
  {
		if(isExecutableConfigured()) 
    {
			return getHGExecutable();
		}
		else 
    {
			if (configureIfMissing) 
      {
				configureExecutable();
				return getHGExecutable();
			}
			else 
      {
				return "hg";
			}
		}
	}

	public static void configureExecutable() 
  {
		Shell shell = Display.getCurrent().getActiveShell();
		String pageId = "com.vectrace.MercurialEclipse.prefspage";
		String[] dsplIds = null;
		Object data = null;
		PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell, pageId, dsplIds, data);
		dlg.open();
	}

	static String search4MercurialRoot(final IProject project) 
  {
		return MercurialUtilities.search4MercurialRoot(project.getLocation().toFile());
	}

	static String search4MercurialRoot(final File file) {
		String path = null;
		File parent = file;
		File hgFolder = new File(parent, ".hg");
		// System.out.println("pathcheck:" + parent.toString());
		while ((parent != null)	&& !(hgFolder.exists() && hgFolder.isDirectory())) 
    {
			parent = parent.getParentFile();
			if (parent != null) 
      {
				// System.out.println("pathcheck:" + parent.toString());
				hgFolder = new File(parent, ".hg");
			}
		}
		if (parent != null) 
    {
			path = hgFolder.getParentFile().toString();
		}
    else 
    {
			path = null;
		}
		// System.out.println("pathcheck: >" + path + "<");
		return path;
	}

	static IProject getProject(IStructuredSelection selection) 
  {
		Object obj;
		obj = selection.getFirstElement();
		if ((obj != null) && (obj instanceof IResource)) 
    {
			return ((IResource) obj).getProject();
		}
		return null;
	}

	static String getRepositoryPath(IProject proj) 
  {
		// Get Repository path
		RepositoryProvider provider = RepositoryProvider.getProvider(proj);
		if (provider instanceof MercurialTeamProvider) 
    {
			return (((MercurialTeamProvider) provider).getRepositoryPath());
		} 
    else 
    {
			return null;
		}
	}

	/*
	 * TODO IProcess, ILaunch? Is this what should be used insted of java.io
	 * stuff ???
	 */

	static void ExecuteCommand(String cmd[]) 
  {
		// Setup and run command
		// System.out.println("hg --cwd " + Repository + " status");
		// String launchCmd[] = { "hg","--cwd", Repository ,"status" };
		// System.out.println("ExecuteCommand:" + cmd.toString());

		if (console == null) 
    {
			console = new IOConsole("Mercurial Console", null);
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			manager.addConsoles(new IConsole[] { console });
		}
		if (console_in == null) 
    {
			console_in = console.getInputStream();
		}
		if (console_out == null) 
    {
			console_out = console.newOutputStream();
			if (console_out != null) 
      {
				console_out_printstream = new PrintStream(console_out);
				// console_out_printstream.setColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));

			}
			// console_out_printstream.println("Hello word!");
		}
		try 
    {
			int c;
			Process process = Runtime.getRuntime().exec(cmd);
			InputStream in = process.getInputStream();
			// System.out.println("Output:");
			while ((c = in.read()) != -1) 
      {
				// System.out.print((char)c);
				console_out_printstream.print((char) c);
			}
			in.close();
			// System.out.println("Error:");
			InputStream err = process.getErrorStream();
			while ((c = err.read()) != -1) 
      {
				// System.out.print((char)c);
				console_out_printstream.print((char) c);
			}
			err.close();
			process.waitFor();
			// TODO put output in a window or something
		}
    catch (IOException e) 
    {
			e.printStackTrace();
		} 
    catch (InterruptedException e) 
    {
			e.printStackTrace();
		}
	}

	/*
	 * public void runTest(IOConsole console) { final Display display =
	 * Display.getDefault();
	 * 
	 * final IOConsoleInputStream in = console.getInputStream();
	 * display.asyncExec(new Runnable() { public void run() {
	 * in.setColor(display.getSystemColor(SWT.COLOR_BLUE)); } });
	 * IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
	 * manager.addConsoles(new IConsole[] { console });
	 * 
	 * final IOConsoleOutputStream out = console.newOutputStream();
	 * //$NON-NLS-1$ Display.getDefault().asyncExec(new Runnable() { public void
	 * run() {
	 * out.setColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
	 * out.setFontStyle(SWT.ITALIC); } });
	 * 
	 * PrintStream ps = new PrintStream(out); ps.println("Any text entered
	 * should be echoed back"); //$NON-NLS-1$ for(;;) { byte[] b = new
	 * byte[1024]; int bRead = 0; try { bRead = in.read(b); } catch (IOException
	 * io) { io.printStackTrace(); }
	 * 
	 * try { out.write(b, 0, bRead); ps.println(); } catch (IOException e) {
	 * e.printStackTrace(); } } }
	 */
}

/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author zingo
 * 
 */
public class MercurialUtilities
{

  static IOConsole console;
  static IOConsoleInputStream console_in;
  static IOConsoleOutputStream console_out;
  static PrintStream console_out_printstream; // migth be used by threads
                                              // GetMercurialConsole should be
                                              // used to get this, even
                                              // internally
                                              // GetMercurialConsole() in
                                              // synchronized

  /**
   * This class is full of utilities metods, useful allover the place
   */
  public MercurialUtilities()
  {

  }

  /** ************************* mercurial command *********************** */

  public static boolean isExecutableConfigured()
  {
    try
    {
      Runtime.getRuntime().exec(getHGExecutable());
      return true;
    } catch (IOException e)
    {
      return false;
    }
  }

  /**
   * Returns the executable for hg. If it's not defined, false is returned
   * 
   * @return false if no hg is defined. True if hg executable is defined
   */
  public static String getHGExecutable()
  {
    IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
        .getPreferenceStore();
    // This returns "" if not defined
    String executable = preferenceStore
        .getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
    return executable;

  }

  public static String getHGExecutable(boolean configureIfMissing)
  {
    if (isExecutableConfigured())
    {
      return getHGExecutable();
    } else
    {
      if (configureIfMissing)
      {
        configureExecutable();
        return getHGExecutable();
      } else
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
    PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
        pageId, dsplIds, data);
    dlg.open();
  }

  /*****************************************************************************
   * Should we handle resource ***********************
   * 
   * @param dialog
   *          TODO
   * 
   * Return true if the resource is handled by mercurial. is it a link we do not
   * folow the link (not now anyway mabe later versions will)
   * 
   */

  public static boolean isResourceInReposetory(IResource resource,
      boolean dialog)
  {
    // System.out.println("isResourceInReposetory(" + resource.toString() +
    // ",dialog)" );

    if (resource instanceof IProject)
    {
      return true;
    }

    // Check to se if resource is not in a link
    String linkedParentName = resource.getProjectRelativePath().segment(0);
    if (linkedParentName == null)
    {
      // System.out.println("isResourceInReposetory(" + resource.toString() +
      // ",dialog) linkedParentName=null" );
      return false;
    }
    // else
    // {
    // System.out.println("isResourceInReposetory(" + resource.toString() +
    // ",dialog) linkedParentName=" + linkedParentName );
    // }
    IFolder linkedParent = resource.getProject().getFolder(linkedParentName);
    boolean isLinked = linkedParent.isLinked();

    if (dialog && isLinked)
    {
      Shell shell;
      IWorkbench workbench;

      workbench = PlatformUI.getWorkbench();
      shell = workbench.getActiveWorkbenchWindow().getShell();

      MessageDialog
          .openInformation(shell, "Resource in link URI",
              "The Selected resource is in a link and can't be handled by this plugin sorry!");
    }

    // TODO Follow links and see if they point to another reposetory

    return !isLinked;
  }

  /** ************************* Username *********************** */

  /**
   * Returns the Username for hg. If it's not defined, false is returned
   * 
   * @return false if no hg is defined. True if hg executable is defined
   */
  public static String getHGUsername()
  {
    IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
        .getPreferenceStore();
    // This returns "" if not defined
    String executable = preferenceStore
        .getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);
    return executable;
  }

  public static String getHGUsername(boolean configureIfMissing)
  {
    String uname = getHGUsername();

    if (uname != null)
    {
      return uname;
    } else
    {
      if (configureIfMissing)
      {
        configureUsername();
        return getHGUsername();
      } else
      {
        return System.getProperty("user.name");
      }
    }
  }

  public static void configureUsername()
  {
    Shell shell = Display.getCurrent().getActiveShell();
    String pageId = "com.vectrace.MercurialEclipse.prefspage";
    String[] dsplIds = null;
    Object data = null;
    PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
        pageId, dsplIds, data);
    dlg.open();
  }

  /**
   * ************************* search for a mercurial repository
   * ***********************
   */

  static String search4MercurialRoot(final IProject project)
  {
    if(project!=null)
    {
      if(project.getLocation() != null)
      {
        return MercurialUtilities.search4MercurialRoot(project.getLocation().toFile());
      } 
    }
    return null;
  }

  static String search4MercurialRoot(final File file)
  {
    String path = null;
    File parent = file;
    File hgFolder = new File(parent, ".hg");
    // System.out.println("pathcheck:" + parent.toString());
    while ((parent != null) && !(hgFolder.exists() && hgFolder.isDirectory()))
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

  /**
   * Get the project for the selection (it use the first element)
   * 
   * @param selection
   * @return
   */
  public static IProject getProject(IStructuredSelection selection)
  {
    Object obj;
    obj = selection.getFirstElement();
    if ((obj != null) && (obj instanceof IResource))
    {
      return ((IResource) obj).getProject();
    }
    return null;
  }

  /*
   * Convenience method to return the OS specific path to the repository.
   */
  static public String getRepositoryPath(IProject project)
  {
    return project.getLocation().toOSString();
  }

  /** ************************* Execute external command *********************** */

  /*
   * TODO IProcess, ILaunch? Is this what should be used insted of java.io stuff
   * ???
   */

  /*
   * Execute command and return output of it in an InputStream Error output is
   * sent to the Mercurial Eclipse console that is created if needed
   */
  // static InputStream ExecuteCommandToInputStream(String cmd[])
  // TODO: Combine this with the other execute command.
  // TODO: Proper error handling.
  public static InputStream ExecuteCommandToInputStream(String cmd[],
      File workingDir, boolean consoleOutput)
  {
    class myIOThreadNoOutput implements Runnable
    {
      Reader input;
      boolean consoleOutput;

      public myIOThreadNoOutput(String aName, Reader instream,
          boolean consoleOutput_)
      {
        // setPriority(getPriority()+1); //Set Priorety one above current pos
        // (we want to take care of the input instead of waiting
        input = instream;
        consoleOutput = consoleOutput_;
      }

      public void run()
      {
        int c;
        PrintStream my_console;
        // Thread.currentThread.sleep(100); //Delay 100 ms
        // System.out.println("Error:");
        try
        {
          if (consoleOutput)
          {
            my_console = getMercurialConsoleOutPrintStream();
          } else
          {
            my_console = null;
          }

          while ((c = input.read()) != -1)
          {
            // System.out.print((char)c);
            if (my_console != null)
            {
              my_console.print((char) c);
            }
          }
        }

        catch (IOException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }
    }

    if (false)
    {
      PrintStream my_console = getMercurialConsoleOutPrintStream();

      if (my_console != null)
      {
        my_console.print("ExecuteCommand: ");
        for (int i = 0; i < cmd.length; i++)
        {
          my_console.print(cmd[i] + " ");
        }
        my_console.println();

        my_console.print("workdir: ");
        if (workingDir != null)
        {
          my_console.print(workingDir.toString());
        } else
        {
          my_console.print("<null>");
        }
        my_console.println();
      }
    }

    // Setup and run command
    InputStream in;
    Reader err_unicode;
    try
    {
      // Process process = Runtime.getRuntime().exec(cmd);
      ProcessBuilder pb = new ProcessBuilder(cmd);
      // Map<String, String> env = pb.environment();
      // env.put("VAR1", "myValue");
      // env.remove("OTHERVAR");
      // env.put("VAR2", env.get("VAR1") + "suffix");
      if (workingDir != null)
      {
        pb.directory(workingDir);
      }
      Process process = pb.start();

      in = process.getInputStream();
      err_unicode = new InputStreamReader(process.getErrorStream()); // InputStreamReader
                                                                      // converts
                                                                      // input
                                                                      // to
                                                                      // unicode

      myIOThreadNoOutput errThread = new myIOThreadNoOutput("stderr",
          err_unicode, true); // only to console
      Thread threadErr = new Thread(errThread);
      threadErr.start();
      threadErr.setPriority(Thread.MAX_PRIORITY); // Set Priority one above
                                                  // current pos (we want to
                                                  // take care of the input
                                                  // instead of waiting

      waitFor(process);

      // Let it continue errThread.join(); //wait for the thread to terminate
      // err_unicode.close();
      return in;

    } 
    catch (IOException e)
    {
      e.printStackTrace();
    } 
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  private static void waitFor(Process process) throws InterruptedException
  {
    int poll = 300;
    int maxTries = 10000 / poll;
    // Timeout 10 seconds, poll every 300 ms
    for (int i = 0; i < maxTries; i++)
    {
      try
      {
        Thread.sleep(poll);
        process.exitValue();
        return;
      } catch (IllegalThreadStateException ex)
      {
        // Expected if process hasn't exited yet
      }
    }
    process.destroy();
    throw new IllegalStateException("Mercurial process timeout occured");
  }

  // TODO: Should probably not return null in case of error.
  public static ByteArrayOutputStream ExecuteCommandToByteArrayOutputStream(String cmd[], File workingDir, boolean consoleOutput) throws HgException
  {
    class myIOThread implements Runnable
    {
      Reader input;
      ByteArrayOutputStream output;
      boolean consoleOutput;

      public myIOThread(String aName, Reader instream,
          ByteArrayOutputStream outstream, boolean consoleOutput_)
      {
        // setPriority(Thread.MAX_PRIORITY); //getPriority()+1 //Set Priority
        // one above current pos (we want to take care of the input instead of
        // waiting
        input = instream;
        output = outstream;
        consoleOutput = consoleOutput_;
      }

      public void run()
      {
        int c;
        PrintStream my_console;
        // Thread.currentThread.sleep(100); //Delay 100 ms
        // System.out.println("Error:");
        try
        {
          if (consoleOutput)
          {
            my_console = getMercurialConsoleOutPrintStream();
          } else
          {
            my_console = null;
          }

          while ((c = input.read()) != -1)
          {
            // System.out.print((char)c);
            // output=output + String.valueOf((char)c);
            if (output != null)
            {
              output.write(c);
            }
            if (my_console != null)
            {
              my_console.print((char) c);
            }
          }
        }

        catch (IOException e)
        {
        	MercurialEclipsePlugin.logError(e);        	
          // TODO Auto-generated catch block
//          e.printStackTrace();
        }

      }
    }

    // Setup and run command
    // System.out.println("hg --cwd " + Repository + " status");
    // String launchCmd[] = { "hg","--cwd", Repository ,"status" };
    // System.out.println("ExecuteCommand:" + cmd.toString());

    // output=new String("");
    // InputStream in;

    // GetMercurialConsole().println("ExecuteCommandToByteArrayOutputStream()
    // Enter");

    ByteArrayOutputStream output;
    // ByteArrayOutputStream error;
    Reader in_unicode;
    Reader err_unicode;
    try
    {
      // Process process = Runtime.getRuntime().exec(cmd);
      ProcessBuilder pb = new ProcessBuilder(cmd);
      // Map<String, String> env = pb.environment();
      // env.put("VAR1", "myValue");
      // env.remove("OTHERVAR");
      // env.put("VAR2", env.get("VAR1") + "suffix");
      if (workingDir != null)
      {
        pb.directory(workingDir);
      }
      Process process = pb.start();

      // in = process.getInputStream();
      output = new ByteArrayOutputStream();
      // error = new ByteArrayOutputStream();
      in_unicode = new InputStreamReader(process.getInputStream()); // InputStreamReader
                                                                    // converts
                                                                    // input to
                                                                    // unicode
      err_unicode = new InputStreamReader(process.getErrorStream()); // InputStreamReader
                                                                      // converts
                                                                      // input
                                                                      // to
                                                                      // unicode

      // output = new StringWriter();
      myIOThread inThread = new myIOThread("stdin", in_unicode, output,consoleOutput);
      // myIOThread errThread = new myIOThread("stderr",err_unicode,error,true);
      // //only to console
      myIOThread errThread = new myIOThread("stderr", err_unicode, output, true); // only
                                                                                // to
                                                                                // console
      Thread threadIn = new Thread(inThread);
      Thread threadErr = new Thread(errThread);
      threadIn.setPriority(Thread.MAX_PRIORITY); // Set Priority one above
                                                  // current pos (we want to
                                                  // take care of the input
                                                  // instead of waiting
      threadErr.setPriority(Thread.MAX_PRIORITY); // Set Priority one above
                                                  // current pos (we want to
                                                  // take care of the input
                                                  // instead of waiting

      // GetMercurialConsole().println("Time to start threads");

      threadIn.start();
      threadErr.start();

      threadIn.join(); // wait for the thread to terminate
      threadErr.join(); // wait for the thread to terminate

      // GetMercurialConsole().println("process.waitFor()");

      waitFor(process);

      // int exit = process.exitValue();

      in_unicode.close();
      err_unicode.close();

      // Should probably throw something or at least return the error.
      // if( exit != 0 )
      // {
      // throw new HgException(exit, "Mercurial operation failed: " + error);
      // }
      // else
      // {
      return output;
      // }

    } catch (IOException e)
    {
    	MercurialEclipsePlugin.logError(e);
//      e.printStackTrace();
    } catch (InterruptedException e)
    {
    	MercurialEclipsePlugin.logError(e);
//      e.printStackTrace();
    }
    return null;
  }

  /*
   * Execute a command via the shell. Can throw HgException if the command does
   * not execute correctly. Exception will contain the error stream from the
   * command execution. @returns String containing the successful
   * 
   * TODO: Should log failure. TODO: Should not return null for failure.
   */
  static public String ExecuteCommand(String cmd[], File workingDir, boolean consoleOutput) throws HgException
  {
    // Setup and run command

    PrintStream my_console = getMercurialConsoleOutPrintStream();

    my_console.println("-----------------------");
    if (my_console != null)
    {
      my_console.print("ExecuteCommand: ");
      for (int i = 0; i < cmd.length; i++)
      {
        my_console.print(cmd[i] + " ");
      }
      my_console.println();

      my_console.print("workdir: ");
      if (workingDir != null)
      {
        my_console.print(workingDir.toString());
      } else
      {
        my_console.print("<null>");
      }
      my_console.println();
    }

    ByteArrayOutputStream output;

    // GetMercurialConsole().println("ExecuteCommandToByteArrayOutputStream()");

    output = ExecuteCommandToByteArrayOutputStream(cmd, workingDir, consoleOutput);
    // my_console.print("output: " + output);
    // my_console.println("-----------------------");
    return (output != null) ? output.toString() : null;
  }

  /**
   * @param obj
   * @return Workingdir of object
   * 
   */
  static public File getWorkingDir(IResource obj)
  {

    File workingDir;
    if (obj.getType() == IResource.PROJECT)
    {
      workingDir = (obj.getLocation()).toFile();
    }
    else if (obj.getType() == IResource.FOLDER)
    {
      workingDir = (obj.getLocation()).toFile();
//      workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
    }
    else if (obj.getType() == IResource.FILE)
    {
      workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
    } 
    else
    {
      workingDir = null;
    }

    return workingDir;
  }

  /**
   * @param obj
   * @return Workingdir of object
   * 
   */
  static public File getWorkingDir(File obj)
  {
    // System.out.println("getWorkingDir( " + obj.toString() + ") = " +
    // obj.getAbsolutePath());
    return new File(obj.getPath());
  }

  static public String getResourceName(IResource obj)
  {
    return (obj.getLocation()).lastSegment();
  }

  static public String getResourceName(IResource obj, File workingDir)
  {

    String st = obj.getLocation().toOSString();
    // System.out.println("getResourceName(<" + st + ">,<"+
    // workingDir.getAbsolutePath() + ">) =<" +
    // st.substring(workingDir.getAbsolutePath().length()) + ">");
    if (st.startsWith(workingDir.getAbsolutePath()))
    {
      st = st.substring(workingDir.getAbsolutePath().length()); // Cut of
                                                                // working dir
      // it might start with a path separator char that we want to remove...
      if (st.startsWith(File.separator))
      {
        st = st.substring(File.separator.length());
      }
    }
    return st;
  }

  public static synchronized IOConsole getMercurialConsole()
  {
    if (console != null)
    {
      return console;
    }

    console = new IOConsole("Mercurial Console", null);
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.addConsoles(new IConsole[] { console });

    if (console_in == null)
    {
      console_in = console.getInputStream();
    }
    if (console_out == null)
    {
      console_out = console.newOutputStream();
      // console_out_printstream.println("Hello word!");
    }
    return console;
  }

  static synchronized PrintStream getMercurialConsoleOutPrintStream()
  {
    if (console_out_printstream != null)
    {
      return console_out_printstream;
    }
    if (console == null)
    {
      console = getMercurialConsole();
    }
    if (console_out != null)
    {
      console_out_printstream = new PrintStream(console_out);
      // console_out_printstream.setColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
      return console_out_printstream;
    }
    return null;
  }
  /*
   * TODO public static synchronized IOConsole getBazaarConsole() {
   * 
   * if (console == null) { console = new IOConsole(UITexts.BazaarConsole_name,
   * null); IConsoleManager manager =
   * ConsolePlugin.getDefault().getConsoleManager(); manager.addConsoles(new
   * IConsole[] { console }); } else { return console; } if (console_in == null) {
   * console_in = console.getInputStream(); } if (console_out == null) {
   * console_out = console.newOutputStream(); //
   * console_out_printstream.println("Hello word!"); } return console; // Error }
   * 
   * static synchronized PrintStream getBazaarConsoleOutPrintStream(){
   * if(console == null) { console = getBazaarConsole(); } if (console_out !=
   * null) { console_out_printstream = new PrintStream(console_out); return
   * console_out_printstream; } return null; }
   */

  /*
   * public void runTest(IOConsole console) { final Display display =
   * Display.getDefault();
   * 
   * final IOConsoleInputStream in = console.getInputStream();
   * display.asyncExec(new Runnable() { public void run() {
   * in.setColor(display.getSystemColor(SWT.COLOR_BLUE)); } }); IConsoleManager
   * manager = ConsolePlugin.getDefault().getConsoleManager();
   * manager.addConsoles(new IConsole[] { console });
   * 
   * final IOConsoleOutputStream out = console.newOutputStream(); //$NON-NLS-1$
   * Display.getDefault().asyncExec(new Runnable() { public void run() {
   * out.setColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
   * out.setFontStyle(SWT.ITALIC); } });
   * 
   * PrintStream ps = new PrintStream(out); ps.println("Any text entered should
   * be echoed back"); //$NON-NLS-1$ for(;;) { byte[] b = new byte[1024]; int
   * bRead = 0; try { bRead = in.read(b); } catch (IOException io) {
   * io.printStackTrace(); }
   * 
   * try { out.write(b, 0, bRead); ps.println(); } catch (IOException e) {
   * e.printStackTrace(); } } }
   */
}

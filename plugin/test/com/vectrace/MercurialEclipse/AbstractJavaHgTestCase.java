/*
 * #%L
 * JavaHg
 * %%
 * Copyright (C) 2011 aragost Trifork ag
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package com.vectrace.MercurialEclipse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Assert;

import com.aragost.javahg.BaseRepository;
import com.aragost.javahg.Changeset;
import com.aragost.javahg.HgVersion;
import com.aragost.javahg.Repository;
import com.aragost.javahg.RepositoryConfiguration;
import com.aragost.javahg.RepositoryConfiguration.CachePolicy;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.flags.AddCommandFlags;
import com.aragost.javahg.commands.flags.CommitCommandFlags;
import com.aragost.javahg.commands.flags.UpdateCommandFlags;
import com.aragost.javahg.commands.flags.VersionCommandFlags;
import com.aragost.javahg.internals.AbstractCommand;
import com.aragost.javahg.internals.Server;
import com.aragost.javahg.internals.Utils;
import com.google.common.io.Files;

/**
 * Base class for test cases.
 *
 * TODO: merge with {@link AbstractMercurialTestCase}
 */
public abstract class AbstractJavaHgTestCase extends TestCase {

    // The jul root logger is changed in the initialization of this
    // class. A strong reference must be maintained to the logger. See
    // LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE on
    // http://findbugs.sourceforge.net/bugDescriptions.html
    private static final Logger JUL_ROOT_LOGGER = Logger.getLogger("");

    private static int count = 0;

    private BaseRepository testRepository;

    private BaseRepository testRepository2;

    protected static final RepositoryConfiguration REPO_CONF;
    static {
        REPO_CONF = new RepositoryConfiguration();
        REPO_CONF.setCachePolicy(CachePolicy.WEAK);

        JUL_ROOT_LOGGER.setLevel(Level.WARNING);

        File dir = Files.createTempDir();
        BaseRepository repo = Repository.create(REPO_CONF, dir);
        HgVersion version = VersionCommandFlags.on(repo).execute();
        repo.close();
        try {
            deleteTempDir(dir);
        } catch (IOException e) {
            System.err.println("JavaHg: Failed to remove temp dir: " + dir.getAbsolutePath());
        }

        System.err.println("JavaHg test using Mercurial version: " + version + ", binary: " + REPO_CONF.getHgBin());
    }

    protected static File createMercurialRepository() {
        File dir = Files.createTempDir();
        Server server = new Server(RepositoryConfiguration.DEFAULT.getHgBin());
        server.initMecurialRepository(dir);
        return dir;
    }

    protected static Charset utf8() {
        return Charset.forName("UTF-8");
    }

    protected BaseRepository getTestRepository() {
        if (this.testRepository == null) {
            File dir = Files.createTempDir();
            this.testRepository = Repository.create(REPO_CONF, dir);
        }
        return this.testRepository;
    }

    protected BaseRepository getTestRepository2() {
        if (this.testRepository2 == null) {
            File dir = Files.createTempDir();
            this.testRepository2 = Repository.create(REPO_CONF, dir);
        }
        return this.testRepository2;
    }

    /**
     * Write to a file in the test repository
     *
     * @param name
     * @param content
     * @throws IOException
     */
    protected void writeFile(String name, String content) throws IOException {
        File file = new File(getTestRepository().getDirectory(), name);
        Files.write(content, file, utf8());
    }

    /**
     * Write something to the file in the test repository.
     * <p>
     * Each call to this method will write different content
     *
     * @param name
     * @throws IOException
     */
    protected void writeFile(String name) throws IOException {
        writeFile(name, String.valueOf(count++) + "\n");
    }

    protected void appendFile(String name) throws IOException {
        File file = new File(getTestRepository().getDirectory(), name);
        Files.append(String.valueOf(count++) + "\n", file, utf8());
    }

    /**
     * Read first line of the file
     *
     * @param name
     * @return
     * @throws IOException
     */
    protected String readFile(String name) throws IOException {
        File file = new File(getTestRepository().getDirectory(), name);
        return Files.readFirstLine(file, utf8());
    }

    /**
     * Delete the specified file from the working copy of the test
     * repository.
     *
     * @param name
     */
    protected void deleteFile(String name) {
        File file = new File(getTestRepository().getDirectory(), name);
        boolean deleted = file.delete();
        if (!deleted) {
            throw new RuntimeException("Could not delete: " + file);
        }
    }

    /**
     * Commit the changes in the test repository
     *
     * @throws IOException
     */
    protected Changeset commit() throws IOException {
        Repository repo = getTestRepository();
        AddCommandFlags.on(repo).execute();
        CommitCommand cmd = CommitCommandFlags.on(repo).user("testcase").message("testcase: " + getClass().getName());
        return cmd.execute();
    }

    /**
     * Create a new changeset in the test repository.
     *
     * @return the changeset Created
     * @throws IOException
     */
    public Changeset createChangeset() throws IOException {
        writeFile("dummyFileForCreatingChangesets", String.valueOf(count++));
        return commit();
    }

    /**
     * Update the test repository to the specified changeset
     *
     * @param cs
     * @throws IOException
     */
    protected void update(Changeset cs) throws IOException {
        UpdateCommandFlags.on(getTestRepository()).clean().rev(cs.getNode()).execute();
    }

    @After
    protected void closeTestRepository() throws IOException {
        if (this.testRepository != null) {
            this.testRepository.close();
            deleteTempDir(this.testRepository.getDirectory());
            this.testRepository = null;
        }
        if (this.testRepository2 != null) {
            this.testRepository2.close();
            deleteTempDir(this.testRepository2.getDirectory());
            this.testRepository2 = null;
        }
    }

    /**
     * Return an absolute File object referencing a file in the
     * specified repository.
     *
     * @param repo
     * @param parts
     * @return
     */
    protected static File repoFile(Repository repo, String... parts) {
        File result = repo.getDirectory();
        for (String part : parts) {
            result = new File(result, part);
        }
        return result;
    }

    /**
     * The error text for missing files is different on Windows
     * compared to Linux/Mac
     *
     * @return
     */
    protected static String getMissingFileErrorText() {
        String error = "No such file or directory";
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            error = "The system cannot find the file specified";
        }
        return error;
    }

    /**
     * Create a temp directory, and return the canonical file object
     * (i.e. no symlinks).
     *
     * @return
     * @throws IOException
     */
    protected static File createTempDir() throws IOException {
        return Files.createTempDir().getCanonicalFile();
    }

    /**
     * Delete a directory in the system temporary directory
     * (java.io.tmpdir).
     *
     * @throws IOException
     */
    protected static void deleteTempDir(File file) throws IOException {
        Utils.deleteTempDir(file);
    }

    protected static void assertSingleton(Object obj, Collection<?> coll) {
        Assert.assertEquals(obj, Utils.single(coll));
    }

    protected static void assertFailedExecution(AbstractCommand cmd) {
        assertFailedExecution(cmd, "");
    }

    protected static void assertFailedExecution(AbstractCommand cmd, String msg) {
        if (msg.length() > 0) {
            msg = " Message: " + msg;
        }
        Assert.fail("Exception expected! Return code: " + cmd.getReturnCode() + "." + msg);
    }

}

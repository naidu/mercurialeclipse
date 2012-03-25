/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.io.IOException;
import java.util.List;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.flags.AddCommandFlags;
import com.aragost.javahg.commands.flags.CommitCommandFlags;
import com.aragost.javahg.commands.flags.LogCommandFlags;
import com.aragost.javahg.commands.flags.MergeCommandFlags;
import com.aragost.javahg.commands.flags.ResolveCommandFlags;
import com.aragost.javahg.commands.flags.UpdateCommandFlags;
import com.vectrace.MercurialEclipse.AbstractJavaHgTestCase;
import com.vectrace.MercurialEclipse.history.GraphLayout.GraphRow;
import com.vectrace.MercurialEclipse.history.GraphLayout.RowAccessor;

/**
 *
 */
public class GraphLayoutTests extends AbstractJavaHgTestCase {

	private static final int ONES_26 = ~0 >>> (32 - 26);

	private static final int ONES_12 = 0xfff;

	public static void testBitTwiddling() {
		RowAccessor la = new RowAccessor(new long[1]);

		assertEquals(0, la.getRevision(0));
		assertEquals(0, la.getParentIndex(0, 0));
		assertEquals(0, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertFalse(la.isDot(0));

		try {
			la.setRevision(0, ~0);
			fail("Cannot use negative index");
		} catch (IllegalStateException e) {
		}

		la.setRevision(0, ONES_26);
		assertEquals(ONES_26, la.getRevision(0));
		assertEquals(0, la.getParentIndex(0, 0));
		assertEquals(0, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertFalse(la.isDot(0));

		la.setDot(0, true);
		assertEquals(ONES_26, la.getRevision(0));
		assertEquals(0, la.getParentIndex(0, 0));
		assertEquals(0, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertTrue(la.isDot(0));

		la.setRevision(0, 12);
		assertEquals(12, la.getRevision(0));
		assertEquals(0, la.getParentIndex(0, 0));
		assertEquals(0, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertTrue(la.isDot(0));

		la.setParentIndex(0, 0, ONES_12);
		assertEquals(12, la.getRevision(0));
		assertEquals(ONES_12, la.getParentIndex(0, 0));
		assertEquals(0, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertTrue(la.isDot(0));

		la.setParentIndex(0, 0, 19);
		la.setParentIndex(0, 1, 54);

		assertEquals(12, la.getRevision(0));
		assertEquals(19, la.getParentIndex(0, 0));
		assertEquals(54, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertTrue(la.isDot(0));

		la.setParentIndex(0, 1, ONES_12);
		la.setDot(0, false);
		assertEquals(12, la.getRevision(0));
		assertEquals(19, la.getParentIndex(0, 0));
		assertEquals(ONES_12, la.getParentIndex(0, 1));
		assertEquals(0, la.getColor(0));
		assertFalse(la.isDot(0));

		la.setColor(0, ONES_12);
		assertEquals(12, la.getRevision(0));
		assertEquals(19, la.getParentIndex(0, 0));
		assertEquals(ONES_12, la.getParentIndex(0, 1));
		assertEquals(ONES_12, la.getColor(0));
		assertFalse(la.isDot(0));

		la.setColor(0, 69);
		assertEquals(12, la.getRevision(0));
		assertEquals(19, la.getParentIndex(0, 0));
		assertEquals(ONES_12, la.getParentIndex(0, 1));
		assertEquals(69, la.getColor(0));
		assertFalse(la.isDot(0));
	}

	private Repository makeTrivialRepo() throws IOException {
		Repository repo = getTestRepository();
		writeFile("x", "abc");

		CommitCommand commit = CommitCommandFlags.on(repo);
		AddCommandFlags.on(repo).execute();

		commit.message("line1\nline2\nX").user("user").execute();

		return repo;
	}

	private void editAndCommit(String contents) throws IOException {
		writeFile("x", contents);
		commit();
	}

	private Changeset[] getLog() {
		Repository repo = getTestRepository();
		List<Changeset> changesets = LogCommandFlags.on(repo).execute();

		return changesets.toArray(new Changeset[changesets.size()]);
	}

	private void update(int revision) throws IOException {
		UpdateCommandFlags.on(getTestRepository()).clean().rev(revision + ":" + revision).execute();
	}

	private void merge(int revision) throws IOException {
		MergeCommandFlags.on(getTestRepository()).force().rev(revision + ":" + revision).execute();
		ResolveCommandFlags.on(getTestRepository()).all().mark();
		commit();
	}

	public void testTrivial() throws IOException {
		GraphLayout graph = new GraphLayout();

		makeTrivialRepo();

		graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		assertEquals(1, graph.numRows());

		GraphRow row = graph.getRow(0);

		assertEquals(1, row.numColumns());
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(0, row.numParents(0));
	}

	public void testTrivial2() throws IOException {
		GraphLayout graph = new GraphLayout();

		makeTrivialRepo();
		editAndCommit("123");

		graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		assertEquals(2, graph.numRows());

		GraphRow row = graph.getRow(0);

		assertEquals(1, row.numColumns());
		assertEquals(1, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(1, row.numParents(0));
		assertEquals(0, row.getParentIndex(0, 0));

		row = graph.getRow(1);

		assertEquals(1, row.numColumns());
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(0, row.numParents(0));
	}

	public void testTwoHead() throws IOException {
		GraphLayout graph = new GraphLayout();

		makeTrivialRepo();
		editAndCommit("123");
		update(0);
		editAndCommit("456");

		graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		assertEquals(3, graph.numRows());

		GraphRow row = graph.getRow(0);

		assertEquals(1, row.numColumns());
		assertEquals(2, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(1, row.numParents(0));
		assertEquals(0, row.getParentIndex(0, 0));
		assertEquals(1, row.getColor(0));

		row = graph.getRow(1);

		assertEquals(2, row.numColumns());
		assertEquals(1, row.getRevision(1));
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(1));
		assertEquals(1, row.numParents(0));
		assertEquals(1, row.getColor(0));
		assertEquals(2, row.getColor(1));

		row = graph.getRow(2);

		assertEquals(1, row.numColumns());
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(0, row.numParents(0));
	}

	public void testDiamond() throws IOException {
		GraphLayout graph = new GraphLayout();

		makeTrivialRepo(); // 0
		editAndCommit("123"); // 1
		update(0);
		editAndCommit("456"); // 2
		merge(1); // 3

		graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		assertEquals(4, graph.numRows());

		GraphRow row = graph.getRow(0);

		assertEquals(1, row.numColumns());
		assertEquals(3, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(2, row.numParents(0));
		assertEquals(0, row.getParentIndex(0, 0));
		assertEquals(1, row.getParentIndex(0, 1));
		assertEquals(1, row.getColor(0));

		row = graph.getRow(1);

		assertEquals(2, row.numColumns());
		assertEquals(2, row.getRevision(0));
		assertEquals(1, row.getRevision(1));
		assertTrue(row.isDot(0));
		assertEquals(1, row.numParents(0));
		assertEquals(0, row.getParentIndex(0, 0));
		assertEquals(1, row.getColor(0));
		assertEquals(2, row.getColor(1));

		row = graph.getRow(2);

		assertEquals(2, row.numColumns());
		assertEquals(1, row.getRevision(1));
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(1));
		assertEquals(1, row.numParents(0));
		assertEquals(1, row.getColor(0));
		assertEquals(2, row.getColor(1));

		row = graph.getRow(3);

		assertEquals(1, row.numColumns());
		assertEquals(0, row.getRevision(0));
		assertTrue(row.isDot(0));
		assertEquals(0, row.numParents(0));
		assertEquals(1, row.getColor(0));
	}

	/**
	 * <pre>
	 * @          5 - 3 and 4
	 * |\
	 * | o        4 - 2 and 1
	 * | |\
	 * o---+      3 - 1 and 2
	 * |/ /
	 * | o        2
	 * | |
	 * o |        1
	 * |/
	 * o          0
	 * </pre>
	 */
	public void testX() throws IOException {
		GraphLayout graph = new GraphLayout();

		makeTrivialRepo(); // 0
		editAndCommit("123"); // 1
		update(0);
		editAndCommit("456"); // 2
		merge(1); // 3
		update(1);
		merge(2); //4
		update(3);

		//graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		//assertEquals(5, graph.numRows());

		merge(4); // 5

		graph = new GraphLayout();
		graph.add(getLog(), null, GraphLayout.ROOT_PARENT_PROVIDER);

		assertEquals(6, graph.numRows());
		assertEquals("[*5(1)>0&1]", graph.getRow(0).toString());
		assertEquals("[3(1)>0, *4(2)>1&2]", graph.getRow(1).toString());
		assertEquals("[*3(1)>0&1, 1(2)>1, 2(3)>0]", graph.getRow(2).toString());
		assertEquals("[*2(1)>0, 1(4)>1]", graph.getRow(3).toString());
		assertEquals("[0(1)>0, *1(4)>0]", graph.getRow(4).toString());
		assertEquals("[*0(4)]", graph.getRow(5).toString());
	}
}

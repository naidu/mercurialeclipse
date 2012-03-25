/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy 	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import com.aragost.javahg.Changeset;
import com.google.common.base.Function;

/**
 * Graph layout algorithm
 */
public class GraphLayout {

	public static ParentProvider ROOT_PARENT_PROVIDER = new ParentProvider() {
		public Changeset[] getParents(Changeset cs) {

			if (cs == null) {
				return new Changeset[0];
			}

			Changeset p1 = cs.getParent1();
			Changeset p2 = cs.getParent2();

			if (p1 == null) {
				return new Changeset[0];
			}

			if (p2 == null) {
				return new Changeset[] { p1 };
			}

			return new Changeset[] { p1, p2 };
		}
	};

	/**
	 * cellBits[rowIndex][column]
	 *
	 * cellBits is in the following format
	 *
	 * <pre>
	 * +DIIIIIIIIIIIIIIIIIIIIIIIIIISSSSSSSSSSSSssssssssssssCCCCCCCCCCCC
	 * + is unused
	 * D is the dot bit I is the node index (revision number) - 26 bits
	 * S is the first successor index in the row below (parent 1) - 12 bits
	 * s is the second successor index in the row below (parent 2) - 12 bits
	 * C is the color - 12 bits
	 * </pre>
	 *
	 * See the {@link RowAccessor}.
	 */
	protected long[][] graph;

	/**
	 * The current color index
	 */
	private int currentColor;

	// operations

	/**
	 * TODO: parent provider should be final at constructor time
	 */
	public void add(Changeset[] changesets, Changeset lastCs, ParentProvider parentProvider) {

		int oldGraphLen = (graph == null) ? 0 : graph.length;
		long[][] newGraph = new long[oldGraphLen + changesets.length][];
		RowAccessor last = new RowAccessor((graph == null) ? new long[0]
				: graph[graph.length - 1]);

		if (graph != null) {
			System.arraycopy(graph, 0, newGraph, 0, graph.length);
		}

		for (int batchIndex = 0; batchIndex < changesets.length; batchIndex++) {
			Changeset curCs = changesets[batchIndex];
			LayoutRowOperation s = new LayoutRowOperation(last, curCs,
					parentProvider.getParents(lastCs));

			last = s.run();
			newGraph[oldGraphLen + batchIndex] = last.row;
			lastCs = curCs;
		}

		graph = newGraph;
	}

	protected int nextColor() {
		currentColor += 1;
		currentColor %= 0xfff;

		return currentColor;
	}

	public int numRows() {
		return graph.length;
	}

	/**
	 * @param row The row number. Zero is the tip
	 * @return The row data
	 */
	public GraphRow getRow(int row) {
		return new GraphRow(row);
	}

	// inner types

	public interface ParentProvider {
		public Changeset[] getParents(Changeset cs);
	}

	/**
	 * Sets last's successors and creates current with no successors set
	 */
	private class LayoutRowOperation {

		private final RowAccessor current;
		private final RowAccessor last;
		private final Changeset currentCs;

		/**
		 * Whether successor has been set in last per column
		 */
		private final boolean[] lastHandled;

		/**
		 * Whether node, color has been set in current per column
		 */
		private final boolean[] currentHandled;

		/**
		 * Index in last of it's change set
		 */
		private int lastsIndex;

		/**
		 * Index in current of it's change set
		 */
		private int currentsIndex;

		private final Changeset[] lastsParents;

		/**
		 * True if the corresponding column in last is a child of the current revision
		 */
		private final boolean[] currentsChildren;

		public LayoutRowOperation(RowAccessor last, Changeset currentCs,
				Changeset[] lastsParents) {
			this.last = last;
			this.lastHandled = new boolean[last.numColumns()];
			this.currentCs = currentCs;
			this.lastsParents = lastsParents;

			// Find the parent(s) of the last change set
			lastsIndex = -1;

			for (int i = 0; i < last.numColumns(); i++) {
				if (last.isDot(i)) {
					lastsIndex = i;
					break;
				}
			}

			// Find the children of the current change set in last
			currentsChildren = new boolean[last.numColumns()];
			int numForks = 0;
			currentsIndex = -1;

			// Find the locations of the children of the current revision in last
			for (int i = 0; i < last.numColumns(); i++) {
				if (last.getRevision(i) == currentCs.getRevision()) {

					if (currentsIndex == -1) {
						currentsIndex = i;
					}

					currentsChildren[i] = true;
					numForks++;
				}
			}

			for (int i = lastsParents.length - 1; i >= 0; i--) {
				int curParentRev = lastsParents[i].getRevision();

				if (curParentRev == currentCs.getRevision()) {
					if (currentsIndex == -1 || lastsIndex < currentsIndex) {
						currentsIndex = lastsIndex + i;
					}

					currentsChildren[lastsIndex] = true;
					numForks++;
				} else {
					for (int j = 0; j < last.numColumns(); j++) {
						if (last.getRevision(j) == curParentRev) {
							numForks++;
						}
					}
				}
			}

			int numCols = lastsParents.length;

			if (last.numColumns() > 0) {
				numCols += last.numColumns() - 1;
			}

			numCols += 1 - numForks;

			// Put heads at the right
			if (currentsIndex == -1 || currentsIndex >= numCols) {
				currentsIndex = numCols - 1;
			}

			current = new RowAccessor(new long[numCols]);
			currentHandled = new boolean[current.numColumns()];
		}

		public RowAccessor run() {

			// Handle the current rev and apply forks from it - current corresponds to zero or more
			// cells in last
			{
				int color = -1;

				for (int li = 0; li < currentsChildren.length; li++) {
					if (currentsChildren[li]) {
						last.setParentIndex(li, 0, currentsIndex);
						lastHandled[li] = true;

						if (color == -1) {
							color = last.getColor(li);
						}
					}
				}

				if (color == -1) {
					// New head
					color = nextColor();
				}

				current.setRevision(currentsIndex, currentCs.getRevision());
				current.setColor(currentsIndex, color);
				current.setDot(currentsIndex, true);
				currentHandled[currentsIndex] = true;
			}

			// Do simple 1-1 copies
			// Each unhandled cell in current corresponds to one or two cells in last
			// All those that correspond to two in last are handled again in the next section
			outer:
			for (int li = 0; li < lastHandled.length; li++) {
				for (int ci = 0; ci < currentHandled.length; ci++) {
					if (!lastHandled[li] && !currentHandled[ci] && li != lastsIndex) {

						last.setParentIndex(li, 0, ci);
						current.setRevision(ci, last.getRevision(li));
						current.setColor(ci, last.getColor(li));
						lastHandled[li] = true;
						currentHandled[ci] = true;

						ci++;
						continue outer;
					}
				}
			}

			// Handle lastsParents: may correspond to zero to two cells in current.
			// May or may not be handled depending on whether currentCs is a parent
			// of lastCs.
			for (int p = 0; p < lastsParents.length; p++) {
				int parentRev = lastsParents[p].getRevision();
				// The color of this lastIndex gets copied to the first parent
				int color = (p == 0) ? last.getColor(lastsIndex) : nextColor();
				boolean handled = false;

				// Look for existing cells that this parent is already assigned to
				for (int ci = 0; ci < currentHandled.length; ci++) {
					if (currentHandled[ci]) {
						if (current.getRevision(ci) == parentRev) {
							last.setParentIndex(lastsIndex, p, ci);
							current.setColor(ci, color); // overwrite color
							handled = true;

							break;
						}
					}
				}

				// Insert new cells for this parent as close as possible to lastIndex
				if (!handled) {
					for (int ci = 0; ci < currentHandled.length; ci++) {
						if (!currentHandled[ci]) {
							last.setParentIndex(lastsIndex, p, ci);
							current.setRevision(ci, parentRev);
							current.setColor(ci, color);
							currentHandled[ci] = true;
							handled = true;

							break;
						}
					}
				}

				assert handled;
				lastHandled[lastsIndex] = true;
			}

			assert nextFalse(currentHandled) < 0;
			assert nextFalse(lastHandled) < 0;

			reorder();

			// Initialize successor for current
			for (int i = 0; i < current.numColumns(); i++) {
				current.setParentIndex(i, 0, RowAccessor.NO_PARENT);
				current.setParentIndex(i, 1, RowAccessor.NO_PARENT);
			}

			return current;
		}

		/**
		 * Reorder cells corresponding to lastParents so they're as close as possible to lastIndex
		 * and in the correct order.
		 */
		private void reorder() {
			ReorderCandidate[] reorder = getReorderCandidates();
			int lastScore = Integer.MAX_VALUE;
			int score = score(reorder);

			while (score < lastScore && score != 0) {
				final ReorderCandidate best = best(reorder);
				final int newCi = best.ci - best.delta;

				current.shiftRight(best.ci, newCi);

				mapLastsIndexes(new Function<Integer, Integer>(){
					public Integer apply(Integer index) {
						if (index == best.ci) {
							index = newCi;
						} else if (newCi <= index && index < best.ci) {
							index = index + 1;
						}
						return index;
					}
				});

				lastScore = score;
				reorder = getReorderCandidates();
				score = score(reorder);
			}
		}

		private void mapLastsIndexes(Function<Integer, Integer> fun) {
			for (int li = 0; li < lastHandled.length; li++) {
				for (int p = 0; p < last.numParents(li); p++) {
					int ci = last.getParentIndex(li, p);

					last.setParentIndex(li, p, fun.apply(ci));
				}
			}
		}

		private ReorderCandidate[] getReorderCandidates() {
			ReorderCandidate[] reorder = new ReorderCandidate[currentHandled.length];

			for (int li = 0; li < lastHandled.length; li++) {
				for (int p = 0; p < last.numParents(li); p++) {
					int index = last.getParentIndex(li, p);
					int target = Math.min(li, currentHandled.length - 1);

					if (p > 0) {
						int otherParentIndex = last.getParentIndex(li, p - 1);

						if (otherParentIndex == index - 1 || target == otherParentIndex) {
							target += 1;
						}
					}

					if (target > index) {
						target = index;
					}

					reorder[index] = new ReorderCandidate(index, index - target).min(reorder[index]);
				}
			}

			return reorder;
		}

		private int score(ReorderCandidate[] ar) {
			int sum = 0;

			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					sum += ar[i].delta;
				}
			}

			return sum;
		}

		private ReorderCandidate best(ReorderCandidate[] ar) {
			int max = 0;
			int maxIndex = -1;

			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null && ar[i].delta > max) {
					max = ar[i].delta;
					maxIndex = i;
				}
			}

			return ar[maxIndex];
		}

		private int nextFalse(boolean[] ar) {
			for (int i = 0; i < ar.length; i++) {
				if (!ar[i]) {
					return i;
				}
			}
			return -1;
		}
	}

	public class GraphRow extends RowAccessor {
		private final int index;

		public GraphRow(int index) {
			super(graph[index]);
			this.index = index;
		}

		public GraphRow getPrevious() {
			return (index > 0) ? new GraphRow(index - 1) : null;
		}

		public int getParentColor(int col, int parentNum) {
			RowAccessor la = new RowAccessor(graph[index + 1]);

			return la.getColor(getParentIndex(col, parentNum));
		}

		public int getDot() {
			for (int i = 0; i < numColumns(); i++) {
				if (isDot(i)) {
					return i;
				}
			}

			return -1;
		}
	}

	/**
	 * Bit twiddling functions for a row
	 */
	protected static class RowAccessor {

		protected static final int NO_PARENT = 0xfff;

		private final long[] row;

		public RowAccessor(long[] row) {
			this.row = row;
		}

		// operations

		protected int getRevision(int col) {
			return (int) ((row[col] << 2) >>> (36 + 2));
		}

		protected void setRevision(int col, int index) {
			if (index < 0) {
				throw new IllegalStateException();
			}

			long val = row[col];

			val &= 0xC000000FFFFFFFFFl;
			val |= (index & 0x3FFFFFFl) << 36;

			row[col] = val;
		}

		protected boolean isDot(int col) {
			return (row[col] & (1l << 62)) != 0;
		}

		protected void setDot(int col, boolean set) {
			if (set) {
				row[col] |= 0x4000000000000000l;
			} else {
				row[col] &= 0xbfffffffffffffffl;
			}
		}

		/**
		 * @param val
		 *            The value
		 * @param parentNum
		 *            Either 0 or 1 - the parent number
		 * @param parentIndex
		 *            Index in the row below of the successor of this (parent)
		 */
		protected void setParentIndex(int col, int parentNum, int parentIndex) {
			switch (parentNum) {
			case 0:
				row[col] &= 0xFFFFFFFFFFFFF000l;
				row[col] |= mask12(parentIndex);
				break;
			case 1:
				row[col] &= 0xFFFFFFFFFF000FFFl;
				row[col] |= mask12(parentIndex) << 12;
				break;
			default:
				throw new IllegalStateException();
			}
		}

		protected int getParentIndex(int col, int parentNum) {
			switch (parentNum) {
			case 0:
				return (int) (row[col] & 0x0000000000000FFFl);
			case 1:
				return (int) ((row[col] & 0x0000000000FFF000l) >>> 12);
			}
			throw new IllegalStateException();
		}

		protected void setColor(int col, int color) {
			row[col] &= 0xFFFFFFF000FFFFFFl;
			row[col] |= mask12(color) << 24;
		}

		protected int getColor(int col) {
			return (int) ((row[col] & 0x0000000FFF000000l) >>> 24);
		}

		private static long mask12(long val) {
			return val & 0x0000000000000FFFl;
		}

		public int numColumns() {
			return row.length;
		}

		public int numParents(int col) {
			if (getParentIndex(col, 0) == NO_PARENT) {
				return 0;
			}
			if (getParentIndex(col, 1) == NO_PARENT) {
				return 1;
			}
			return 2;
		}

		protected void shiftLeft(int col) {
			long cur = row[col];

			for (col = col + 1; col < row.length; col++) {
				row [col - 1] = row[col];
			}

			row[row.length - 1] = cur;
		}

		/**
		 * Move the cell at fromCol to toCol and shift cells in between right.
		 */
		protected void shiftRight(int fromCol, int toCol) {
			assert toCol <= fromCol;

			long from = row[fromCol];

			for (int i = fromCol - 1; i >= toCol; i--) {
				row[i + 1] = row[i];
			}

			row[toCol] = from;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder(64);

			buf.append('[');

			for (int i = 0; i < row.length; i++) {
				if (i != 0) {
					buf.append(", ");
				}
				if (isDot(i)) {
					buf.append('*');
				}
				buf.append(getRevision(i));
				buf.append('(');
				buf.append(getColor(i));
				buf.append(')');

				if (getParentIndex(i, 0) != NO_PARENT) {
					buf.append('>');
					buf.append(getParentIndex(i, 0));
					if (getParentIndex(i, 1) != NO_PARENT) {
						buf.append('&');
						buf.append(getParentIndex(i, 1));
					}
				}
			}

			buf.append(']');

			return buf.toString();
		}
	}

	private static class ReorderCandidate {

		public final int delta;
		public final int ci;

		public ReorderCandidate(int ci, int delta) {
			this.ci = ci;
			this.delta = delta;
		}

		public ReorderCandidate min(ReorderCandidate other) {
			if (other != null && other.delta < this.delta) {
				return other;
			}

			return this;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ci + "-" + delta;
		}
	}
}

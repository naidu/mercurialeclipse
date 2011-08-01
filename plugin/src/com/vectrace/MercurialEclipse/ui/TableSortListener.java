package com.vectrace.MercurialEclipse.ui;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Sorts a table. Should listen to column selection
 */
@SuppressWarnings("unchecked")
public abstract class TableSortListener implements Listener {

	private final Comparator[] comparators;
	private final Table table;

	public TableSortListener(Table table, Comparator[] comparators) {
		this.table = table;
		this.comparators = comparators;
	}

	public void handleEvent(Event e) {
		if (!canSort()) {
			MessageDialog.openInformation(table.getShell(), "Table not loaded",
					"Cannot sort because the table is not fully loaded");
			return;
		}

		// determine new sort column and direction
		TableColumn sortColumn = table.getSortColumn();
		TableColumn currentColumn = (TableColumn) e.widget;
		int dir = table.getSortDirection();
		if (sortColumn == currentColumn) {
			dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
		} else {
			table.setSortColumn(currentColumn);
			dir = SWT.UP;
		}
		// sort the data based on column and direction
		int nIdx = 0;
		for (TableColumn c : table.getColumns()) {
			if (c == currentColumn) {
				break;
			}
			nIdx++;
		}

		Object[] data = getData();

		Arrays.sort(data, comparators[nIdx]);

		if (dir == SWT.DOWN) {
			for (int i = 0, n = data.length; i < n / 2; i++) {
				Object temp = data[i];
				data[i] = data[n - i - 1];
				data[n - i - 1] = temp;
			}
		}

		// update data displayed in table
		table.setSortDirection(dir);
		table.clearAll();
	}

	/**
	 * @return False if the data is not fully loaded and so can't be sorted
	 */
	protected boolean canSort() {
		return true;
	}

	/**
	 * @return The data. Will be sorted in place
	 */
	protected abstract Object[] getData();

	public static int sort(int a, int b) {
		if (a == b) {
			return 0;
		}
		return a < b ? -1 : 1;
	}
}
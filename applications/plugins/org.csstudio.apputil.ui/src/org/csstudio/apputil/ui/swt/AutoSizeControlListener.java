package org.csstudio.apputil.ui.swt;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/** Auto-size table columns to fill the available space.
 *  <p>
 *  All the table columns must be generated by AutoSizeColumn.make(..)
 *  which attaches information about the desired minimum size
 *  and resize weight to each column.
 *  <p>
 *  The AutoSizeControlListener then adds itself as a ControlListener
 *  to the table and performs the automated resizing.
 *  <p> 
 *  Related ideas are in the Eclipse 3.4 sources for
 *  org.eclipse.ui.views.properties.PropertySheetViewer
 *  and 
 *  org.eclipse.jface.internal.ConfigureColumnsDialog.
 *  That code, however, is 'internal' API and only performs
 *  one initial resizing.
 *  
 *  @author Kay Kasemir
 */
public class AutoSizeControlListener
	extends ControlAdapter implements DisposeListener
{
	private static final int BORDER_STUFF = 4;
	final private Table table;
	final private boolean only_once;
	private boolean autosize = false;
	/** Flag to remember initial run to allow one Auto-size on initial run */
    private boolean initial_run = true;

    /** Constructor.
     *  @param container Typically the Parent of the table. No longer used!
     *  @param table The table to resize automatically
     *  @deprecated Use <code>AutoSizeControlListener(final Table table)</code
     *              instead
     */
    public AutoSizeControlListener(final Composite container, final Table table)
	{
	    this(table, false);
	}

    /** Constructor.
     *  @param table The table to resize automatically
     */
    public AutoSizeControlListener(final Table table)
    {
        this(table, false);
    }

    /** Constructor.
     *  @param table The table to resize automatically
     *  @param only_once Disable auto-resize after initial resize
     *                   (to be re-enabled by user via AutoSizeColumnAction?)
     */
    @SuppressWarnings("nls")
    public AutoSizeControlListener(final Table table, final boolean only_once)
	{
		this.table = table;
		this.only_once = only_once;
		for (int i = 0; i < table.getColumnCount(); ++i)
		{
			final TableColumn column = table.getColumn(i);
            final Object data = column.getData();
			if (data == null)
			    throw new Error("Column " + column.getText() + "(" + i
			            + ") has null data");
            if (!(data instanceof AutoSizeColumn))
				throw new Error("Column " + column.getText() + "(" + i
				        + ") has invalid data type " + data.getClass().getName());
		}
		// Listen to container resize ...
		table.addControlListener(this);
		// .. until the table gets removed.
		table.addDisposeListener(this);
	}

    /** @param enable Enable or disable the auto-sizing? */
	void enableAutosize(final boolean enable)
    {
        autosize = enable;
        if (autosize)
            controlResized(null);
    }

	/** @return <code>true</code> if auto-sizing */
    boolean isAutosizing()
    {
        return autosize;
    }

    /** Resize the table columns. */
	@Override
	public void controlResized(final ControlEvent ignored)
	{
        if (only_once && !autosize && !initial_run)
            return;
        initial_run = false;

		int i, total_weight = 0, total_min = 0;
		// Compute sum of all weights and minimum sizes.
		for (i = 0; i < table.getColumnCount(); ++i)
		{
			final TableColumn col = table.getColumn(i);
			total_weight += ((AutoSizeColumn) col.getData()).getWeight();
			total_min += ((AutoSizeColumn) col.getData()).getMinSize();
		}
		// Resize columns to fit total
		final Rectangle bounds = table.getClientArea();
		// Adjust width to allow for borders, avoid scroll bars
		bounds.width -= BORDER_STUFF;
		if (bounds.width < 50)
			bounds.width = 50;
		int extra = bounds.width - total_min;
		if (extra < 0)
			extra = 0;
		// Assign minimum size to all columns,
		// distributing extra spaced based on column weights.
		for (i = 0; i < table.getColumnCount(); ++i)
		{
		    final TableColumn col = table.getColumn(i);
			int size = ((AutoSizeColumn) col.getData()).getMinSize();
			if (extra > 0)
			{
				int weight = ((AutoSizeColumn) col.getData()).getWeight();
				size += (extra * weight) / total_weight;
			}
			col.setWidth(size);
		}
	}

	/** DisposeListener */
	public void widgetDisposed(final DisposeEvent e)
	{
		if (table.isDisposed())
			return;
		table.removeControlListener(this);
	}
}

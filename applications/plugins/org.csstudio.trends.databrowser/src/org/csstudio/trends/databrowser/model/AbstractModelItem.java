/**
 * 
 */
package org.csstudio.trends.databrowser.model;

import java.util.ArrayList;

import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.platform.model.IProcessVariable;
import org.csstudio.swt.chart.TraceType;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.swt.graphics.Color;

/** Base class for IModelItem implementations
 *  @author Kay Kasemir
 */
public abstract class AbstractModelItem
    extends PlatformObject
    implements IModelItem
{
    // Tag names used to write/read XML
    static final String TAG_PV = "pv"; //$NON-NLS-1$
    static final String TAG_ARCHIVE = "archive"; //$NON-NLS-1$
    static final String TAG_KEY = "key"; //$NON-NLS-1$
    static final String TAG_URL = "url"; //$NON-NLS-1$
    static final String TAG_COLOR = "color"; //$NON-NLS-1$
    static final String TAG_TRACE_TYPE = "trace_type"; //$NON-NLS-1$
    static final String TAG_LOG_SCALE = "log_scale"; //$NON-NLS-1$
    static final String TAG_BLUE = "blue"; //$NON-NLS-1$
    static final String TAG_GREEN = "green"; //$NON-NLS-1$
    static final String TAG_RED = "red"; //$NON-NLS-1$
    static final String TAG_AUTOSCALE = "autoscale"; //$NON-NLS-1$
    static final String TAG_VISIBLE = "visible"; //$NON-NLS-1$
    static final String TAG_MAX = "max"; //$NON-NLS-1$
    static final String TAG_MIN = "min"; //$NON-NLS-1$
    static final String TAG_LINEWIDTH = "linewidth"; //$NON-NLS-1$
    static final String TAG_AXIS = "axis"; //$NON-NLS-1$
    static final String TAG_NAME = "name"; //$NON-NLS-1$
    
    /** The model to which this item belongs. */
    final protected Model model;
    
    /** The name of this Chart Item. */
    protected String name;
    
    /** The units of this Chart Item. */
    protected String units = ""; //$NON-NLS-1$
    
    /** The Y axis to use. */
    protected int axis_index;
    
    /** Y-axis range. */
    protected double axis_low, axis_high;
    
    /** Is the item visible? */
    protected boolean visible;
    
    /** Auto scale trace */
    protected boolean auto_scale;

    /** The color for this item.
     *  <p>
     *  Issue:
     *  Using the SWT Color binds the 'model' to the GUI library.
     *  But only keeping red/green/blue data in the 'model'
     *  results in a lot of code for allocating and freeing the Color
     *  all over the place, so after some back and force I decided
     *  to have the color in the model.
     */
    protected Color color;
    
    /** The line width for this item. */
    protected int line_width;
    
    /** The trace type (line, ...) */
    protected TraceType trace_type;
    
    /** Use log scale? */
    protected boolean log_scale;
    
    /** Where to get archived data for this item. */
    protected ArrayList<IArchiveDataSource> archives
        = new ArrayList<IArchiveDataSource>();
    
    AbstractModelItem(Model model, String pv_name,
            int axis_index, double min, double max,
            boolean visible,
            boolean auto_scale,
            int red, int green, int blue,
            int line_width,
            TraceType trace_type,
            boolean log_scale)
    {
        this.model = model;
        name = pv_name;
        this.axis_index = axis_index;
        this.axis_low = min;
        this.axis_high = max;
        this.visible = visible;
        this.auto_scale = auto_scale;
        this.color = new Color(null, red, green, blue);
        this.line_width = line_width;
        this.trace_type = trace_type;
        this.log_scale = log_scale;
    }
    
    /** @return Model to which this item belongs. */
    public Model getModel()
    {   return model;    }
    
    /** Must be called to dispose the color. */
    public void dispose()
    {
        color.dispose();
        archives.clear();
        archives = null;
    }
    
    /** @see IProcessVariable */
    public final String getTypeId()
    {   return IProcessVariable.TYPE_ID;   }

    /** @see IModelItem#getName() */
    public final String getName()
    {   return name;  }
    
    /** Base implementation for changing the name.
     *  Derived classes might also need to change PV names etc.
     *  @see IModelItem#changeName(String)
     */
    public void changeName(String new_name)
    {
        // Avoid duplicates, do not allow if new name already in model.
        if (model.findEntry(new_name) >= 0)
            return;
        // Name change looks like remove/add back in
        model.fireEntryRemoved(this);
        // Now change name
        name = new_name;
        // and add
        model.fireEntryAdded(this);
    }
    
    /** @see IModelItem#getUnits() */
    public final String getUnits()
    {   return units;  }

    /** @see IModelItem#getAxisIndex() */
    public final int getAxisIndex()
    {   return axis_index;  }
    
    /** @see IModelItem#setAxisIndex(int) */
    public final void setAxisIndex(int axis)
    {
        if (axis_index == axis)
            return;
        axis_index = axis;
        // Adapt to axis limits and type of other model items on this axis
        for (int i=0; i<model.getNumItems(); ++i)
        {
            IModelItem item = model.getItem(i);
            if (item != this  &&  item.getAxisIndex() == axis_index)
            {
                setAxisLimitsSilently(item.getAxisLow(), item.getAxisHigh());
                setLogScaleSilently(item.getLogScale());
                break;
            }
        }
        model.fireEntryConfigChanged(this);
    }
    
    /** @see IModelItem#getAxisLow() */
    public final double getAxisLow()
    {   return axis_low; }

    /** @see IModelItem#getAxisHigh() */
    public final double getAxisHigh()
    {   return axis_high; }

    /** @see IModelItem#setAxisLow(double) */
    public final void setAxisLow(double limit)
    {   
        axis_low = limit;
        model.setAxisLimits(axis_index, axis_low, axis_high);
    }

    /** @see IModelItem#setAxisHigh(double) */
    public final void setAxisHigh(double limit)
    {   
        axis_high = limit;
        model.setAxisLimits(axis_index, axis_low, axis_high);
    }

    /** Set axis limits, but don't inform model.
     *  <p>
     *  Used by model to avoid recursion that would result from setAxisMin/Max.
     */
    public final void setAxisLimitsSilently(double low, double high)
    {
        axis_low = low;
        axis_high = high;
    }

    /** @see IModelItem#isVisible() */
    public final boolean isVisible()
    {
        return visible;
    }
    
    /** @see IModelItem#setVisible(boolean) */
    public final void setVisible(boolean yesno)
    {
        if (visible == yesno)
            return; // no change
        visible = yesno;
        // Notify model of this change.
        model.fireEntryConfigChanged(this);
    }
    
    /** @see IModelItem#setAutoScale(boolean) */
    public final void setAutoScale(boolean auto_scale) 
    {
        // Notify model of this change.
        model.setAutoScale(axis_index, auto_scale);
    }
    
    /** Configure to use auto scale or not.
     *  <p>
     *  For internal use by the model to avoid recursion
     *  as would happen with setAutoScale.
     *  @see #setLogScale(boolean)
     */
    final void setAutoScaleSilently(boolean auto_scale) 
    {
        this.auto_scale = auto_scale;
    }
    
    /** @see IModelItem#getAutoScale() */
    public final boolean getAutoScale()
    {   return auto_scale; }
    
    /** @see IModelItem#getColor() */
    public final Color getColor()
    {   return color; }
    
    /** @see IModelItem#setColor(Color) */
    public final void setColor(Color new_color)
    {
        color.dispose();
        color = new_color;
        // Notify model of this change.
        model.fireEntryConfigChanged(this);
    }
    
    /** @return Returns the trace line width. */
    public final int getLineWidth()
    {
        return line_width;
    }
    
    /** Set the trace to a new line width. */
    public final void setLineWidth(int new_width)
    {
        line_width = new_width;
        // Notify model of this change.
        model.fireEntryConfigChanged(this);
    }
    
    /** @return Returns current model display type */
    public final TraceType getTraceType() 
    {
        return trace_type;
    } 
    
    /** Set new display type */
    public final void setTraceType(TraceType new_trace_type) 
    {
        if(trace_type == new_trace_type)
            return;
        trace_type = new_trace_type;
        // Notify model of this change.
        model.fireEntryConfigChanged(this);
    }
    
    /** @return <code>true</code> if using log. scale */
    public final boolean getLogScale()
    {
        return log_scale;
    }

    /** Configure to use log. scale or not. */
    public final void setLogScale(boolean use_log_scale)
    {
        // Notify model of this change.
        model.setLogScale(axis_index, use_log_scale);
    }

    /** Configure to use log. scale or not.
     *  <p>
     *  For internal use by the model to avoid recursion
     *  as would happen with setLogScale.
     *  @see #setLogScale(boolean)
     */
    final void setLogScaleSilently(boolean use_log_scale)
    {
        log_scale = use_log_scale;
    }
    
    /** @see IModelItem#getArchiveDataSources() */
    public final IArchiveDataSource[] getArchiveDataSources()
    {
        IArchiveDataSource result[] = new IArchiveDataSource[archives.size()];
        return archives.toArray(result);
    }
    
    /** Add another archive data source. */
    public void addArchiveDataSource(IArchiveDataSource archive)
    {
        // Ignore duplicates
        for (IArchiveDataSource arch : archives)
            if (arch.getKey() == archive.getKey() &&
                arch.getUrl().equals(archive.getUrl()))
                return;
        archives.add(archive);
        // Notify model of this change.
        model.fireEntryArchivesChanged(this);
    }

    /** Remove given archive data source. */
    public final void removeArchiveDataSource(IArchiveDataSource archive)
    {
        // Remove all matching entries (should be at most one...)
        for (int i = 0; i < archives.size(); ++i)
        {
            IArchiveDataSource entry = archives.get(i);
            if (entry.getKey() == archive.getKey() &&
                entry.getUrl().equals(archive.getUrl()))
                archives.remove(i); // changes size(), but that's OK
        }
        // Notify model of this change.
        model.fireEntryArchivesChanged(this);
    }
    
    /** Move given archive data source 'up' in the list. */
    public final void moveArchiveDataSourceUp(IArchiveDataSource archive)
    {
        // Move first matching entry, _skipping_ the top one!
        for (int i = 1/*!*/; i < archives.size(); ++i)
        {
            IArchiveDataSource entry = archives.get(i);
            if (entry.getKey() == archive.getKey() &&
                entry.getUrl().equals(archive.getUrl()))
            {
                entry = archives.remove(i);
                archives.add(i-1, entry);
                // Notify model of this change.
                model.fireEntryArchivesChanged(this);
                return;
            }
        }
    }
    
    /** Move given archive data source 'down' in the list. */
    public final void moveArchiveDataSourceDown(IArchiveDataSource archive)
    {
        // Move first matching entry, _skipping_ the last entry!
        for (int i = 0; i < archives.size()-1/*!*/; ++i)
        {
            IArchiveDataSource entry = archives.get(i);
            if (entry.getKey() == archive.getKey() &&
                entry.getUrl().equals(archive.getUrl()))
            {
                entry = archives.remove(i);
                archives.add(i+1, entry);
                // Notify model of this change.
                model.fireEntryArchivesChanged(this);
                return;
            }
        }
    }
}

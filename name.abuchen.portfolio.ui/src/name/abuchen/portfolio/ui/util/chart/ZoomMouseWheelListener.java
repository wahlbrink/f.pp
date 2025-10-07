package name.abuchen.portfolio.ui.util.chart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.Range;

public class ZoomMouseWheelListener implements Listener
{
    private final static double ZOOM_RATIO = 0.1;

    public static void attachTo(Chart chart, boolean requireMod)
    {
        Listener listener = new ZoomMouseWheelListener(chart, requireMod);
        chart.getPlotArea().getControl().addListener(SWT.MouseVerticalWheel, listener);
    }

    private final Chart chart;

    private final int requiredMod;

    private ZoomMouseWheelListener(Chart chart, boolean requireMod)
    {
        this.chart = chart;
        this.requiredMod = (requireMod) ? SWT.MOD1 : 0;
    }

    @Override
    public void handleEvent(Event event)
    {
        if (!event.doit || (requiredMod != 0 && event.stateMask != requiredMod))
            return;
        event.doit = false;

        for (IAxis axis : chart.getAxisSet().getYAxes())
        {
            double coordinate = axis.getDataCoordinate(event.y);
            Range range = axis.getRange();

            double lower = 0;
            double upper = 0;

            if (event.count > 0)
            {
                lower = range.lower + 2 * ZOOM_RATIO * (coordinate - range.lower);
                upper = range.upper + 2 * ZOOM_RATIO * (coordinate - range.upper);
            }
            else
            {
                lower = (range.lower - 2 * ZOOM_RATIO * coordinate) / (1 - 2 * ZOOM_RATIO);
                upper = (range.upper - 2 * ZOOM_RATIO * coordinate) / (1 - 2 * ZOOM_RATIO);
            }

            axis.setRange(new Range(lower, upper));
        }
        chart.redraw();
    }
}

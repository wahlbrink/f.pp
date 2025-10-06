package name.abuchen.portfolio.ui.views.columns;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.DataType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.util.TextUtil;

public class NameColumn extends Column
{

    public static final String DEFAULT_ID = "name"; //$NON-NLS-1$

    private static final Function<Object, Named> DEFAULT_GET_NAMED = (e) -> {
        return Adaptor.adapt(Named.class, e);
    };

    public static class NameColumnLabelProvider extends ColumnLabelProvider
    {
        private Function<Object, Named> getNamed;
        private Client client;

        public NameColumnLabelProvider(Function<Object, Named> getNamed, Client client)
        {
            this.getNamed = getNamed;
            this.client = client;
        }

        public NameColumnLabelProvider(Client client)
        {
            this.getNamed = DEFAULT_GET_NAMED;
            this.client = client;
        }

        @Override
        public String getText(Object element)
        {
            Named n = getNamed.apply(element);
            return n != null ? n.getName() : null;
        }

        @Override
        public Image getImage(Object element)
        {
            Named n = getNamed.apply(element);
            return LogoManager.instance().getDefaultColumnImage(n, client.getSettings());
        }

        @Override
        public String getToolTipText(Object element)
        {
            Named n = getNamed.apply(element);
            if (n == null)
                return null;
            else if (n instanceof Security security)
                return TextUtil.wordwrap(security.toInfoString());
            else
                return TextUtil.tooltip(n.getName());
        }
    }

    public NameColumn(Client client)
    {
        this(DEFAULT_ID, Messages.ColumnName, 300, new NameColumnLabelProvider(client), true);
    }

    public NameColumn(String id, ColumnLabelProvider labelProvider)
    {
        this(id, Messages.ColumnName, 300, labelProvider, true);
    }

    public NameColumn(String id, String label, int defaultWidth, Client client)
    {
        this(id, label, defaultWidth, new NameColumnLabelProvider(client), true);
    }

    public NameColumn(String id, String label, int defaultWidth, ColumnLabelProvider labelProvider)
    {
        this(id, label, defaultWidth, labelProvider, true);
    }

    public NameColumn(String id, String label, int defaultWidth, ColumnLabelProvider labelProvider,
                    boolean enableEditing)
    {
        this(id, DEFAULT_GET_NAMED, label, defaultWidth, labelProvider);
        if (enableEditing)
            addEditingSupport(null);
    }

    public NameColumn(String id, Function<Object, Named> getNamed, String label, int defaultWidth, Client client)
    {
        this(id, getNamed, label, defaultWidth, new NameColumnLabelProvider(getNamed, client));
    }

    public NameColumn(String id, Function<Object, Named> getNamed, String label, int defaultWidth,
                    ColumnLabelProvider labelProvider)
    {
        super(id, DataType.NAME, label, SWT.LEFT, defaultWidth);

        if (getNamed != DEFAULT_GET_NAMED && labelProvider instanceof NameColumnLabelProvider nameLabelProvider
                        && nameLabelProvider.getNamed == DEFAULT_GET_NAMED)
            nameLabelProvider.getNamed = getNamed;
        setLabelProvider(labelProvider);
        setCompareBy((e) -> {
            Named n = getNamed.apply(e);
            return (n != null) ? n.getName() : null;
        });
    }

    private void addEditingSupport(ModificationListener listener)
    {
        var editingSupport = new StringEditingSupport(Named.class, "name").setMandatory(true); //$NON-NLS-1$
        if (listener != null)
            editingSupport.addListener(listener);
        editingSupport.attachTo(this);
    }

}

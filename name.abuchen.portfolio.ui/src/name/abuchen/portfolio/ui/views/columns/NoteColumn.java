package name.abuchen.portfolio.ui.views.columns;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.ui.DataType;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.util.TextUtil;

public class NoteColumn extends Column
{

    public static final String DEFAULT_ID = "note"; //$NON-NLS-1$

    private static final Function<Object, String> DEFAULT_GET_NOTE = (e) -> {
        Annotated annotated = Adaptor.adapt(Annotated.class, e);
        if (annotated == null)
            annotated = Adaptor.adapt(Named.class, e);
        return (annotated != null) ? annotated.getNote() : null;
    };

    public NoteColumn()
    {
        this(DEFAULT_ID, true);
    }

    public NoteColumn(String id)
    {
        this(id, true);
    }

    public NoteColumn(String id, boolean enableEditing)
    {
        this(id, DEFAULT_GET_NOTE, Messages.ColumnNote);
        if (enableEditing)
            addEditingSupport(null);
    }

    public NoteColumn(Function<Object, String> getNode)
    {
        this(DEFAULT_ID, getNode, Messages.ColumnNote);
    }

    private NoteColumn(String id, Function<Object, String> getNote, String label)
    {
        super(id, DataType.OTHER_TEXT, (label != null) ? label : Messages.ColumnNote, SWT.LEFT, 200);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                String note = getNote.apply(e);
                return note == null || note.isEmpty() ? null : TextUtil.toSingleLine(note);
            }

            @Override
            public Image getImage(Object e)
            {
                String note = getNote.apply(e);
                return note != null && note.length() > 0 ? Images.NOTE.image() : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                String note = getNote.apply(e);
                return note == null || note.isEmpty() ? null : TextUtil.wordwrap(note);
            }

        });
        setCompareBy(getNote);
    }

    private void addEditingSupport(ModificationListener listener)
    {
        var editingSupport = new StringEditingSupport(Annotated.class, "note"); //$NON-NLS-1$
        if (listener != null)
            editingSupport.addListener(listener);
        editingSupport.attachTo(this);
    }

}

package name.abuchen.portfolio.ui.views.payments;

import java.text.MessageFormat;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.DataType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

public class TransactionsTab implements PaymentsTab
{
    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    @Inject
    private PaymentsViewModel model;

    @Inject
    private SelectionService selectionService;

    @Inject
    private IPreferenceStore preferences;

    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelTransactions;
    }

    @Override
    public void addExportActions(IMenuManager manager)
    {
        manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.LabelTransactions))
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(tableViewer).export(Messages.LabelTransactions + ".csv"); //$NON-NLS-1$
            }
        });
    }

    @Override
    public Control createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);

        ShowHideColumnHelper support = new ShowHideColumnHelper(TransactionsTab.class.getSimpleName() + "@v3", //$NON-NLS-1$
                        preferences, tableViewer, layout);

        addColumns(support);
        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.addSelectionChangedListener(event -> {
            TransactionPair<?> tx = ((TransactionPair<?>) ((IStructuredSelection) event.getSelection())
                            .getFirstElement());

            view.setInformationPaneInput(tx);

            if (tx != null && tx.getTransaction().getSecurity() != null)
                selectionService.setSelection(
                                new SecuritySelection(model.getClient(), tx.getTransaction().getSecurity()));
        });

        tableViewer.setInput(model.getTransactions());

        model.addUpdateListener(() -> tableViewer.setInput(model.getTransactions()));

        return container;
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new Column(DataType.DATE, Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new DateTimeLabelProvider(
                        element -> ((TransactionPair<?>) element).getTransaction().getDateTime()));
        column.setComparator(TransactionPair.BY_DATE);
        column.setSortAsDefault();
        support.addColumn(column);

        Function<Object, String> tx2type = element -> ((TransactionPair<?>) element)
                        .getTransaction() instanceof AccountTransaction
                                        ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                                        .getType().toString()
                                        : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                                        .getType().toString();

        column = new Column(DataType.TRANSACTION_TYPE, Messages.ColumnTransactionType, SWT.LEFT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return tx2type.apply(element);
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        ColumnViewerSorter.createIgnoreCase(tx2type, column.getDataType()).attachTo(column);
        support.addColumn(column);

        column = new NameColumn("securityName", Messages.ColumnSecurity, 250, model.getClient()); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(DataType.NUM_SHARES, Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                return ((TransactionPair<?>) element).getTransaction().getShares();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        column.setCompareBy(e -> ((TransactionPair<?>) e).getTransaction().getShares());
        support.addColumn(column);

        column = new Column(DataType.MONEY, Messages.ColumnGrossValue, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Money transactionGrossValue = ((TransactionPair<?>) element)
                                .getTransaction() instanceof AccountTransaction
                                                ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                                                .getGrossValue()
                                                : ((PortfolioTransaction) ((TransactionPair<?>) element)
                                                                .getTransaction()).getGrossValue();
                return Values.Money.format(transactionGrossValue, client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        column.setCompareBy(element -> ((TransactionPair<?>) element).getTransaction() instanceof AccountTransaction
                        ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getGrossValue()
                        : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getGrossValue());
        support.addColumn(column);

        column = new Column(DataType.MONEY, Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Money transactionTaxes = ((TransactionPair<?>) element).getTransaction() instanceof AccountTransaction
                                ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                                .getUnitSum(Unit.Type.TAX)
                                : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                                .getUnitSum(Unit.Type.TAX);
                return Values.Money.format(transactionTaxes, client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        column.setCompareBy(element -> ((TransactionPair<?>) element).getTransaction() instanceof AccountTransaction
                        ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getUnitSum(Unit.Type.TAX)
                        : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getUnitSum(Unit.Type.TAX));
        support.addColumn(column);

        column = new Column(DataType.MONEY, Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Money transactionFees = ((TransactionPair<?>) element).getTransaction() instanceof AccountTransaction
                                ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                                .getUnitSum(Unit.Type.FEE)
                                : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                                .getUnitSum(Unit.Type.FEE);
                return Values.Money.format(transactionFees, client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        column.setCompareBy(element -> ((TransactionPair<?>) element).getTransaction() instanceof AccountTransaction
                        ? ((AccountTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getUnitSum(Unit.Type.FEE)
                        : ((PortfolioTransaction) ((TransactionPair<?>) element).getTransaction())
                                        .getUnitSum(Unit.Type.FEE));
        support.addColumn(column);

        column = new Column(DataType.MONEY, Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((TransactionPair<?>) element).getTransaction().getMonetaryAmount(),
                                client.getBaseCurrency());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor(element);
            }
        });
        column.setCompareBy(e -> ((TransactionPair<?>) e).getTransaction().getMonetaryAmount());
        support.addColumn(column);

        column = new Column(DataType.NAME, Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getOwner().toString();
            }

            @Override
            public Image getImage(Object element)
            {
                Object owner = ((TransactionPair<?>) element).getOwner();
                return LogoManager.instance().getDefaultColumnImage(owner, model.getClient().getSettings());
            }
        });
        column.setCompareBy(e -> ((TransactionPair<?>) e).getOwner().toString());
        support.addColumn(column);

        column = new NoteColumn(null, false);
        support.addColumn(column);

        column = new Column(DataType.NAME, Messages.ColumnSource, SWT.None, 200);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TransactionPair<?>) element).getTransaction().getSource();
            }
        });
        column.setCompareBy(e -> ((TransactionPair<?>) e).getTransaction().getSource());
        column.setVisible(false);
        support.addColumn(column);
    }

    private Color colorFor(Object element)
    {
        TransactionPair<?> tx = (TransactionPair<?>) element;
        if (tx.getTransaction() instanceof AccountTransaction)
        {
            return ((AccountTransaction) tx.getTransaction()).getType().isCredit() ? Colors.theme().greenForeground()
                            : Colors.theme().redForeground();
        }
        else
        {
            return ((PortfolioTransaction) tx.getTransaction()).getType().isPurchase()
                            ? Colors.theme().greenForeground()
                            : Colors.theme().redForeground();
        }
    }
}

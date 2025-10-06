package name.abuchen.portfolio.ui.views.columns;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.DataType;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;

public class CurrencyColumn extends Column
{

    public static final String DEFAULT_ID = "currency"; //$NON-NLS-1$

    private static final Function<Object, String> DEFAULT_GET_CODE = (e) -> {
        InvestmentVehicle investmentVehicle = Adaptor.adapt(InvestmentVehicle.class, e);
        return (investmentVehicle != null) ? investmentVehicle.getCurrencyCode() : null;
    };

    private static class CurrencyColumnLabelProvider extends ColumnLabelProvider
    {
        private Function<Object, String> getCurrencyCode;

        public CurrencyColumnLabelProvider(Function<Object, String> getCurrencyCode)
        {
            this.getCurrencyCode = getCurrencyCode;
        }

        @Override
        public String getText(Object element)
        {
            return getCurrencyCode.apply(element);
        }
    }

    public static class CurrencyEditingSupport extends ListEditingSupport
    {
        public CurrencyEditingSupport()
        {
            super(InvestmentVehicle.class, "currencyCode", //$NON-NLS-1$
                            CurrencyUnit.getAvailableCurrencyUnits().stream() //
                                            .map(u -> u.getCurrencyCode()).sorted().collect(Collectors.toList()));
        }

    }

    public CurrencyColumn()
    {
        this(DEFAULT_ID, DEFAULT_GET_CODE, Messages.ColumnCurrency);
    }

    public CurrencyColumn(String id, Function<Object, String> getCurrencyCode, String label)
    {
        super(id, DataType.CURRENCY, label, SWT.LEFT, 60);

        setLabelProvider(new CurrencyColumnLabelProvider(getCurrencyCode));
        setCompareBy(getCurrencyCode);
    }

}

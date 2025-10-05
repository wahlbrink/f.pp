package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.DividendFeed;
import name.abuchen.portfolio.util.WebAccess;

public class DivvyDiaryDividendFeed implements DividendFeed
{
    private String apiKey;

    @VisibleForTesting
    PageCache<JSONArray> cache = new PageCache<>();

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DividendEvent> getDividendPayments(Security security) throws IOException
    {
        if (apiKey == null)
            return Collections.emptyList();

        String isin = security.getIsin();
        if (Strings.isNullOrEmpty(isin))
            return Collections.emptyList();

        JSONArray dividends = cache.lookup(isin);
        if (dividends == null)
        {
            String json = createWebAccess("api.divvydiary.com", "/symbols/" + isin) //$NON-NLS-1$ //$NON-NLS-2$
                            .addHeader("X-API-Key", apiKey) //$NON-NLS-1$
                            .addUserAgent("PortfolioPerformance/" //$NON-NLS-1$
                                            + FrameworkUtil.getBundle(DivvyDiaryDividendFeed.class).getVersion()
                                                            .toString())
                            .get();

            JSONObject jsonObject = (JSONObject) JSONValue.parse(json);
            if (jsonObject == null)
            {
                throw new IOException("server returned data that doesn't seem to be JSON"); //$NON-NLS-1$
            }

            dividends = (JSONArray) jsonObject.get("dividends"); //$NON-NLS-1$
            if (dividends == null)
            {
                throw new IOException("server returned an unexpected JSON-format"); //$NON-NLS-1$
            }

            cache.put(isin, dividends);
        }

        List<DividendEvent> answer = new ArrayList<>();
        dividends.forEach(entry -> {
            JSONObject row = (JSONObject) entry;

            DividendEvent payment = new DividendEvent();

            payment.setDate(YahooHelper.fromISODate((String) row.get("exDate"))); //$NON-NLS-1$
            payment.setPaymentDate(YahooHelper.fromISODate((String) row.get("payDate"))); //$NON-NLS-1$
            payment.setAmount(Money.of((String) row.get("currency"), //$NON-NLS-1$
                            Values.Amount.factorize(((Number) row.get("amount")).doubleValue()))); //$NON-NLS-1$

            payment.setSource("divvydiary.com"); //$NON-NLS-1$

            answer.add(payment);
        });
        return answer;
    }

    @VisibleForTesting
    WebAccess createWebAccess(String host, String path)
    {
        return new WebAccess(host, path);
    }
}

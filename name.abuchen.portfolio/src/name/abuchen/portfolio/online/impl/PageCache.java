package name.abuchen.portfolio.online.impl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

/* package */ class PageCache<T>
{
    private static class PageEntry<T>
    {
        final long ts;
        final T answer;

        public PageEntry(T prices)
        {
            this.ts = System.nanoTime();
            this.answer = prices;
        }
    }

    private final long expirationTime;

    private SequencedMap<String, PageEntry<T>> map = new LinkedHashMap<String, PageEntry<T>>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PageEntry<T>> eldest)
        {
            PageEntry<T> entry = eldest.getValue();
            return (entry == null || System.nanoTime() - entry.ts > expirationTime);
        }
    };

    public PageCache()
    {
        this.expirationTime = Duration.ofMinutes(5).toNanos();
    }

    public PageCache(Duration expirationTime)
    {
        this.expirationTime = expirationTime.toNanos();
    }

    /**
     * Returns a cached list of security prices for a given URL.
     * 
     * @return list of prices; null if no cache entry exists
     */
    public synchronized T lookup(String url)
    {
        PageEntry<T> entry = map.get(url);
        return (entry == null || System.nanoTime() - entry.ts > expirationTime) ? null : entry.answer;
    }

    public synchronized void put(String url, T prices)
    {
        map.putFirst(url, new PageEntry<>(prices));
    }

}

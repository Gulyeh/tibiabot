package cache.utils;

import java.util.concurrent.ConcurrentHashMap;

public final class UtilsCache {
    private UtilsCache() {}

    public static final ConcurrentHashMap<String, String> wikiGifLinksMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, String> wikiArticlesLinksMap = new ConcurrentHashMap<>();
}

package org.geelato.web.platform.cache;


public interface CacheService<T> {
    void putCache(String key, T value);

    T getCache(String key);

    void removeCache(String key);
}

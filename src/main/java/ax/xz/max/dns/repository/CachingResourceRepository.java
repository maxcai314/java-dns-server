package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.List;

public class CachingResourceRepository implements ResourceRepository {
	private final ResourceRepository delegate;
	private volatile boolean isClosed = false;

	public CachingResourceRepository(ResourceRepository delegate, int cacheSize) {
		this.delegate = delegate;
		this.cache = new LimitedCache<>(cacheSize);
		this.chainCache = new LimitedCache<>(cacheSize);
	}

	public CachingResourceRepository(ResourceRepository delegate) {
		this(delegate, 100);
	}

	public static CachingResourceRepository of(ResourceRepository delegate) {
		return new CachingResourceRepository(delegate);
	}

	private void throwIfClosed() throws ResourceAccessException {
		if (isClosed) throw new ResourceAccessException("Repository is closed");
	}

	public void flushCache() {
		cache.clear();
		chainCache.clear();
	}

	// cached
	private record CacheKey(DomainName name, short type) {}
	private final LimitedCache<CacheKey, List<ResourceRecord>> cache;
	private final LimitedCache<CacheKey, List<AliasChain>> chainCache;
	@Override
	public List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		var key = new CacheKey(name, type);
		var result = cache.get(key);
		if (result == null) {
			result = delegate.getAllByNameAndType(name, type);
			cache.put(key, result);
		}
		return result;
	}

	@Override
	public List<AliasChain> getAllChainsByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		var key = new CacheKey(name, type);
		var result = chainCache.get(key);
		if (result == null) {
			result = delegate.getAllChainsByNameAndType(name, type);
			chainCache.put(key, result);
		}
		return result;
	}

	// no cache
	@Override
	public void clear() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			delegate.clear();
		} finally {
			flushCache();
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			delegate.insert(record);
		} finally {
			flushCache();
		}
	}

	@Override
	public List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			return delegate.delete(record);
		} finally {
			flushCache();
		}
	}

	@Override
	public List<ResourceRecord> getAll() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		return delegate.getAll();
	}

	@Override
	public List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		return delegate.getAllByName(name);
	}

	@Override
	public List<ResourceRecord> deleteAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			return delegate.deleteAllByName(name);
		} finally {
			flushCache();
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			return delegate.deleteAllByNameAndType(name, type);
		} finally {
			flushCache();
		}
	}

	@Override
	public List<ResourceRecord> getAllByType(short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		return delegate.getAllByType(type);
	}

	@Override
	public List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		try {
			return delegate.deleteAllByType(type);
		} finally {
			flushCache();
		}
	}

	@Override
	public void close() throws ResourceAccessException {
		isClosed = true;
		delegate.close();
	}
}

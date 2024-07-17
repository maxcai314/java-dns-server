package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FilteredResourceRepository implements ResourceRepository {
	private final ResourceRepository delegate;
	private volatile boolean isClosed = false;
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
	private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
	private BloomFilter<DomainName> nameFilter;

	private static final double STARTING_ERROR_RATE = 0.000_001;
	private static final double MINIMUM_ERROR_RATE = 0.000_01;

	public FilteredResourceRepository(ResourceRepository delegate) throws ResourceAccessException, InterruptedException {
		this.delegate = delegate;
		resetFilter();
	}

	private void throwIfClosed() throws ResourceAccessException {
		if (isClosed) throw new ResourceAccessException("Repository is closed");
	}

	public void resetFilter() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			List<DomainName> names = delegate.getAllDomainNames();
			nameFilter = BloomFilter.optimalFilterFor(names.size(), STARTING_ERROR_RATE);
			names.forEach(nameFilter::add);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			if (nameFilter.neverContains(name))
				return List.of();
			else
				return delegate.getAllByNameAndType(name, type);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public List<AliasChain> getAllChainsByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			if (nameFilter.neverContains(name))
				return List.of();
			else
				return delegate.getAllChainsByNameAndType(name, type);
		} finally {
			readLock.unlock();
		}
	}

	// no cache
	@Override
	public void clear() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			delegate.clear();
			nameFilter.clear();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			delegate.insert(record);
			nameFilter.add(record.name());
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			var result = delegate.delete(record);
			resetFilter(); // not strictly necessary, but helpful
			return result;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> getAll() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			return delegate.getAll();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public List<DomainName> getAllDomainNames() throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			return delegate.getAllDomainNames();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			if (nameFilter.neverContains(name))
				return List.of();
			else
				return delegate.getAllByName(name);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			var result = delegate.deleteAllByName(name);
			resetFilter(); // not strictly necessary, but helpful
			return result;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.lock();
		try {
			var result = delegate.deleteAllByNameAndType(name, type);
			resetFilter(); // not strictly necessary, but helpful
			return result;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> getAllByType(short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		readLock.lock();
		try {
			return delegate.getAllByType(type);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException, InterruptedException {
		throwIfClosed();
		writeLock.unlock();
		try {
			var result = delegate.deleteAllByType(type);
			resetFilter(); // not strictly necessary, but helpful
			return result;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() throws ResourceAccessException {
		isClosed = true;
	}
}

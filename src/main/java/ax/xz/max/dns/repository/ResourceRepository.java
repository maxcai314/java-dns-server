package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.CNameRecord;
import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.List;

public interface ResourceRepository extends AutoCloseable {
	void clear() throws ResourceAccessException, InterruptedException;

	void insert(ResourceRecord record) throws ResourceAccessException, InterruptedException;
	List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException, InterruptedException;
	List<ResourceRecord> getAll() throws ResourceAccessException, InterruptedException;

	List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException, InterruptedException;
	List<ResourceRecord> deleteAllByName(DomainName name) throws ResourceAccessException, InterruptedException;

	List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException;
	List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException;

	List<ResourceRecord> getAllByType(short type) throws ResourceAccessException, InterruptedException;
	List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException, InterruptedException;
	List<AliasChain> getAllChainsByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException;

	@Override
	void close() throws ResourceAccessException;

	record AliasChain(CNameRecord aliasRecord, ResourceRecord record) {
		public AliasChain {
			if (!aliasRecord.alias().equals(record.name()))
				throw new IllegalArgumentException("CName alias mismatch: " + aliasRecord.alias() + " != " + record.name());
		}
	}
}

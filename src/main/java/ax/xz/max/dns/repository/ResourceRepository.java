package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.List;

public interface ResourceRepository {
	void clear() throws ResourceAccessException;

	void insert(ResourceRecord record) throws ResourceAccessException;
	List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException;
	List<ResourceRecord> getAll() throws ResourceAccessException;

	List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException;
	List<ResourceRecord> deleteAllByName(ResourceRecord record) throws ResourceAccessException;

	List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException;
	List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException;

	List<ResourceRecord> getAllByType(short type) throws ResourceAccessException;
	List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException;
}

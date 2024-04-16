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

	<T extends ResourceRecord> List<T> getAllByNameAndType(DomainName name, Class<T> type) throws ResourceAccessException;
	<T extends ResourceRecord> List<T> deleteAllByNameAndType(DomainName name, Class<T> type) throws ResourceAccessException;

	<T extends ResourceRecord> List<T> getAllByType(Class<T> type) throws ResourceAccessException;
	<T extends ResourceRecord> List<T> deleteAllByType(Class<T> type) throws ResourceAccessException;
}

package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.ARecord;
import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.Collection;
import java.util.Optional;

public interface ResourceRepository {
	void clear() throws ResourceAccessException;

	void insert(ResourceRecord record) throws ResourceAccessException;
	Collection<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException;
	Collection<ResourceRecord> getAll() throws ResourceAccessException;

	Collection<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException;
	Collection<ResourceRecord> deleteAllByName(ResourceRecord record) throws ResourceAccessException;

	<T extends ResourceRecord> Collection<T> getAllByNameAndType(DomainName name, Class<T> clazz) throws ResourceAccessException;
	<T extends ResourceRecord> Collection<T> deleteAllByNameAndType(DomainName name, Class<T> clazz) throws ResourceAccessException;

	<T extends ResourceRecord> Collection<T> getAllByType(Class<T> clazz) throws ResourceAccessException;
	<T extends ResourceRecord> Collection<T> deleteAllByType(Class<T> clazz) throws ResourceAccessException;
}

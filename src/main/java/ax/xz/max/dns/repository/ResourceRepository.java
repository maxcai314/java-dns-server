package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.ARecord;
import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.Collection;
import java.util.Optional;

public interface ResourceRepository {
	void clear() throws ResourceAccessException;

	// todo: shouldn't exist?
	void insert(ResourceRecord record) throws ResourceAccessException;
	void delete(ResourceRecord record) throws ResourceAccessException;
	Collection<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException;
	<T extends ResourceRecord> Collection<T> getAllByNameAndType(DomainName name, Class<T> clazz) throws ResourceAccessException;

}
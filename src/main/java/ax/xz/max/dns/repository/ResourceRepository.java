package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.ARecord;
import ax.xz.max.dns.resource.ResourceRecord;

import java.util.Optional;

public interface ResourceRepository {
	void clear() throws ResourceAccessException;

	// todo: shouldn't exist?
	default void insert(ResourceRecord record) throws ResourceAccessException {
		switch (record) {
			case ARecord aRecord -> insertARecord(aRecord);
			// etc.
		}
	}

	void insertARecord(ARecord aRecord) throws ResourceAccessException;
	Optional<ARecord> getARecord(String domain) throws ResourceAccessException;

	void deleteARecord(String domain) throws ResourceAccessException;

}

package ax.xz.max.dns.database;

import ax.xz.max.dns.resource.ARecord;

public interface ResourceRepository {
	void clear() throws ResourceAccessException;

	void insert(ARecord aRecord) throws ResourceAccessException;

	void deleteARecord(String domain) throws ResourceAccessException;

}

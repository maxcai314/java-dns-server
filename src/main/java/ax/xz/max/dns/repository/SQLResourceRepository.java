package ax.xz.max.dns.repository;

import ax.xz.max.dns.repository.connection.ConnectionFactory;
import ax.xz.max.dns.repository.connection.ConnectionPool;
import ax.xz.max.dns.resource.*;
import org.sqlite.SQLiteDataSource;

import java.lang.foreign.MemorySegment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLResourceRepository implements ResourceRepository {
	private final SQLiteDataSource dataSource;
	private final ConnectionPool connectionPool;
	public SQLResourceRepository() throws ResourceAccessException, InterruptedException {
		dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:records.db");
		connectionPool = new ConnectionPool(ConnectionFactory.of(dataSource), 10, 15);
		initialize();
	}

	private void initialize() throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varbinary(255) NOT NULL, type Integer NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to initialize database", e);
		}
	}

	@Override
	public void clear() throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("DROP TABLE IF EXISTS records");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varbinary(255) NOT NULL, type Integer NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to clear database", e);
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO records (name, type, time_to_live, data) VALUES (?, ?, ?, ?)");
		) {
			statement.setBytes(1, record.name().bytes());
			statement.setShort(2, record.type());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData().asByteBuffer().array());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to insert record", e);
		}
	}

	@Override
	public List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? AND type = ? AND time_to_live = ? AND data = ? RETURNING *");
		) {
			statement.setBytes(1, record.name().bytes());
			statement.setShort(2, record.type());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData().asByteBuffer().array());

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))).domainName(),
							resultSet.getShort("type"),
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? RETURNING type, time_to_live, data");
		) {
			statement.setBytes(1, name.bytes());

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							name,
							resultSet.getShort("type"),
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? AND type = ? RETURNING time_to_live, data");
		) {
			statement.setBytes(1, name.bytes());
			statement.setShort(2, type);

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							name,
							type,
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByType(short type) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT name, time_to_live, data FROM records WHERE type = ?");
		) {
			statement.setShort(1, type);

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))).domainName(),
							type,
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE type = ? RETURNING name, time_to_live, data");
		) {
			statement.setShort(1, type);

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))).domainName(),
							type,
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> getAll() throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM records");
		) {
			List<ResourceRecord> records = new ArrayList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))).domainName(),
						resultSet.getShort("type"),
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT type, time_to_live, data FROM records WHERE name = ?");
		) {
			statement.setBytes(1, name.bytes());

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							name,
							resultSet.getShort("type"),
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT time_to_live, data FROM records WHERE name = ? AND type = ?");
		) {
			statement.setBytes(1, name.bytes());
			statement.setShort(2, type);

			try (ResultSet resultSet = statement.executeQuery()) {
				List<ResourceRecord> records = new ArrayList<>();

				while (resultSet.next())
					records.add(ResourceRecord.fromData(
							name,
							type,
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					));

				return records;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<AliasChain> getAllChainsByNameAndType(DomainName name, short type) throws ResourceAccessException, InterruptedException {
		try (
				Connection connection = connectionPool.acquireConnection();
				PreparedStatement statement = connection.prepareStatement("""
						WITH RECURSIVE candidates AS ( SELECT data, time_to_live FROM records WHERE name = ? AND type = ? )
						SELECT records.name as name, records.time_to_live as time_to_live, records.data as data, candidates.time_to_live as candidate_time_to_live FROM records 
						CROSS JOIN candidates ON records.name = candidates.data WHERE records.type = ?;""");
		) {
			statement.setBytes(1, name.bytes());
			statement.setShort(2, CNameRecord.ID);
			statement.setShort(3, type);

			try (ResultSet resultSet = statement.executeQuery()) {
				List<AliasChain> chains = new ArrayList<>();

				while (resultSet.next()) {
					ResourceRecord record = ResourceRecord.fromData(
							DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))).domainName(),
							type,
							resultSet.getInt("time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("data"))
					);

					CNameRecord context = CNameRecord.fromData(
							name,
							resultSet.getInt("candidate_time_to_live"),
							MemorySegment.ofArray(resultSet.getBytes("name"))
					);

					chains.add(new AliasChain(context, record));
				}

				return chains;
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get records", e);
		}
	}

	@Override
	public void close() throws ResourceAccessException {
		try {
			connectionPool.close();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to close connection pool", e);
		}
	}
}

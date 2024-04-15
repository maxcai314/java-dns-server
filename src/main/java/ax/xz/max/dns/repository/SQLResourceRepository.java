package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.ResourceRecord;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class SQLResourceRepository implements ResourceRepository {
	private final SQLiteDataSource dataSource;
	public SQLResourceRepository() throws ResourceAccessException {
		dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:records.db");
		initialize();
	}

	private void initialize() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS A_records (hostname Varchar(255) PRIMARY KEY, address Binary(4))");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to initialize database", e);
		}
	}

	@Override
	public void clear() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("DROP TABLE IF EXISTS A_records");
			statement.executeUpdate("CREATE TABLE A_records (hostname Varchar(255) PRIMARY KEY, address Binary(4))");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to clear database", e);
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException {

	}

	@Override
	public void delete(ResourceRecord record) throws ResourceAccessException {

	}

	@Override
	public Collection<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException {
		return null;
	}

	@Override
	public <T extends ResourceRecord> Collection<T> getAllByNameAndType(DomainName name, Class<T> clazz) throws ResourceAccessException {
		return null;
	}
}

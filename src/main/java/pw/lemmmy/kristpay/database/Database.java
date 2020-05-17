package pw.lemmmy.kristpay.database;

import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.database.migrations.Migrations;

import javax.sql.DataSource;
import java.sql.*;
import java.util.OptionalInt;

public class Database {
	private static final String DB_URI = KristPay.INSTANCE.getConfig().getDatabase().getConnectionURI();

	@Getter private DataSource dataSource;
	
	public Database() {
		SqlService sql = Sponge.getServiceManager().provide(SqlService.class).get();
		
		try {
			dataSource = sql.getDataSource(DB_URI);
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error connecting to KristPay database", e);
		}
		
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();
			String dbName = meta.getDatabaseProductName().toLowerCase();
			if (!dbName.contains("mysql") && !dbName.contains("mariadb"))
				throw new RuntimeException("Only MySQL or MariaDB are supported by KristPay as of version 3.0.0.");
			
			Migrations.runMigrations(dataSource, conn);
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error running KristPay database migrations", e);
		}
	}

	public OptionalInt getTotalDistributedKrist() {
		String balanceSum = "SELECT SUM(balance) FROM accounts";

		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(balanceSum);
			return OptionalInt.of(rs.getInt(0));
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error getting total distributed krist", e);
		}

		return OptionalInt.empty();
	}
}

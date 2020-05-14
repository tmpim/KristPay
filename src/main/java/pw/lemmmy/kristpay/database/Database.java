package pw.lemmmy.kristpay.database;

import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
	}

	public OptionalInt getTotalDistributedKrist() {
		String balanceSum = "SELECT SUM(balance) FROM accounts";

		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(balanceSum);
			return OptionalInt.of(rs.getInt(0));
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return OptionalInt.empty();
	}
}

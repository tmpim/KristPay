package pw.lemmmy.kristpay.database;

import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
	private static final String DB_URI = KristPay.INSTANCE.getConfig().getDatabase().getConnectionURI();
	
	private SqlService sql;
	@Getter private DataSource dataSource;
	
	public Database() {
		sql = Sponge.getServiceManager().provide(SqlService.class).get();
		
		try {
			dataSource = sql.getDataSource(DB_URI);
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error connecting to KristPay database", e);
		}
	}
	
	public void load() {
		createTables();
	}
	
	private void createTables() {
		String txLogTable = "CREATE TABLE IF NOT EXISTS tx_log (" +
			"id INT NOT NULL AUTO_INCREMENT," +
			"success BOOLEAN," +
			"error VARCHAR(255)," +
			"type VARCHAR(255)," +
			"from_account VARCHAR(255)," +
			"to_account VARCHAR(255)," +
			"from_address VARCHAR(255)," +
			"dest_address VARCHAR(255)," +
			"to_address VARCHAR(255)," +
			"amount INT," +
			"return_address VARCHAR(255)," +
			"meta_message VARCHAR(255)," +
			"meta_error VARCHAR(255)," +
			"krist_txid INT," +
			"time TIMESTAMP," +
			"PRIMARY KEY (id)" +
		");";
		
		String faucetRewardTable = "CREATE TABLE IF NOT EXISTS faucet_rewards (" +
			"id INT NOT NULL AUTO_INCREMENT," +
			"ip VARCHAR(255)," +
			"account VARCHAR(255)," +
			"value INT," +
			"reward_tier INT," +
			"time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
			"minimum TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
			"expires TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
			"PRIMARY KEY (id)" +
		");";
		
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmtTxLogTable = conn.prepareStatement(txLogTable);
			 PreparedStatement stmtFaucetRewardTable = conn.prepareStatement(faucetRewardTable)) {
			stmtTxLogTable.executeUpdate();
			stmtFaucetRewardTable.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

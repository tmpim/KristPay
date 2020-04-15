package pw.lemmmy.kristpay.database;

import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;

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

		String accountTable = "CREATE TABLE IF NOT EXISTS accounts (" +
			"id INT NOT NULL AUTO_INCREMENT," +
			"deposit_address CHAR(10) NOT NULL," +
			"deposit_password VARCHAR(100) NOT NULL," +
			"owner CHAR(36) NOT NULL," +
			"balance INT NOT NULL," +
			"unseen_deposit INT NOT NULL," +
			"unseen_transfer INT NOT NULL," +
			"welfare_counter INT NOT NULL," +
			"welfare_amount INT NOT NULL DEFAULT -1," +
			"welfare_last_payment TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
			"PRIMARY KEY (id)," +
			"UNIQUE INDEX owner (owner)" +
		");";
		
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement stmtTxLogTable = conn.prepareStatement(txLogTable);
			 PreparedStatement stmtFaucetRewardTable = conn.prepareStatement(faucetRewardTable);
			 PreparedStatement stmtAccountTable = conn.prepareStatement(accountTable)) {
			stmtTxLogTable.executeUpdate();
			stmtFaucetRewardTable.executeUpdate();
			stmtAccountTable.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
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

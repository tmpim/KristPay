package pw.lemmmy.kristpay.database;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
	private static final String DB_URI = KristPay.INSTANCE.getConfig().getDatabase().getConnectionURI();
	
	private SqlService sql;
	private DataSource data;
	
	public Database() {
		sql = Sponge.getServiceManager().provide(SqlService.class).get();
		
		try {
			data = sql.getDataSource(DB_URI);
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
			"from_address VARCHAR(255)," +
			"to_address VARCHAR(255)," +
			"amount INT," +
			"return_address VARCHAR(255)," +
			"meta_message VARCHAR(255)," +
			"meta_error VARCHAR(255)," +
			"krist_txid INT," +
			"PRIMARY KEY (id)" +
		");";
		
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(txLogTable)) {
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addTransactionLogEntry(boolean success, String error, String fromAddress, String toAddress, int
		amount, String returnAddress, String metaMessage, String metaError, int kristTXID) {
		String query = "INSERT INTO tx_log (success, error, from_address, to_address, amount, return_address, " +
			"meta_message, meta_error, krist_txid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setBoolean(1, success);
			stmt.setString(2, error);
			stmt.setString(3, fromAddress);
			stmt.setString(4, toAddress);
			stmt.setInt(5, amount);
			stmt.setString(6, returnAddress);
			stmt.setString(7, metaMessage);
			stmt.setString(8, metaError);
			stmt.setInt(9, kristTXID);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

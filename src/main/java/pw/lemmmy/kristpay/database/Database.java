package pw.lemmmy.kristpay.database;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristTransactionResult;
import pw.lemmmy.kristpay.economy.KristTransferResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
			"PRIMARY KEY (id)" +
		");";
		
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(txLogTable)) {
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addTransactionLogEntry(TransactionResult transactionResult,
									   String fromAccount,
									   String toAccount,
									   String fromAddress,
									   String destAddress,
									   String toAddress,
									   int amount,
									   String returnAddress,
									   String metaMessage,
									   String metaError,
									   int kristTXID) {
		boolean success = transactionResult.getResult().equals(ResultType.SUCCESS);
		String error = transactionResult.getResult().name().toLowerCase();
		String type = transactionResult.getType().getName().toLowerCase();
		
		if (!success && transactionResult instanceof KristTransactionResult && ((KristTransactionResult) transactionResult).getError() != null) {
			error = ((KristTransactionResult) transactionResult).getError();
		}
		
		addTransactionLogEntry(success, error, type, fromAccount, toAccount, fromAddress, destAddress, toAddress,
			amount, returnAddress, metaMessage, metaError, kristTXID);
	}
	
	public void addTransactionLogEntry(TransferResult transferResult,
									   String fromAccount,
									   String toAccount,
									   String fromAddress,
									   String destAddress,
									   String toAddress,
									   int amount,
									   String returnAddress,
									   String metaMessage,
									   String metaError,
									   int kristTXID) {
		boolean success = transferResult.getResult().equals(ResultType.SUCCESS);
		String error = transferResult.getResult().name().toLowerCase();
		String type = transferResult.getType().getName().toLowerCase();
		
		if (!success && transferResult instanceof KristTransferResult && ((KristTransferResult) transferResult).getError() != null) {
			error = ((KristTransferResult) transferResult).getError();
		}
		
		addTransactionLogEntry(success, error, type, fromAccount, toAccount, fromAddress, destAddress, toAddress,
			amount, returnAddress, metaMessage, metaError, kristTXID);
	}
	
	public void addTransactionLogEntry(boolean success,
									   String error,
									   String type,
									   String fromAccount,
									   String toAccount,
									   String fromAddress,
									   String destAddress,
									   String toAddress,
									   int amount,
									   String returnAddress,
									   String metaMessage,
									   String metaError,
									   int kristTXID) {
		Task.builder().execute(() -> {
			String query = "INSERT INTO tx_log (success, error, type, from_account, to_account, from_address, " +
				"dest_address, to_address, amount, return_address, meta_message, meta_error, krist_txid) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			try (Connection conn = data.getConnection();
				 PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setBoolean(1, success);
				stmt.setString(2, error);
				stmt.setString(3, type);
				stmt.setString(4, fromAccount);
				stmt.setString(5, toAccount);
				stmt.setString(6, fromAddress);
				stmt.setString(7, destAddress);
				stmt.setString(8, toAddress);
				stmt.setInt(9, amount);
				stmt.setString(10, returnAddress);
				stmt.setString(11, metaMessage);
				stmt.setString(12, metaError);
				stmt.setInt(13, kristTXID);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}).async().name("KristPay - Transaction log").submit(KristPay.INSTANCE);
	}
}

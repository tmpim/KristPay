package pw.lemmmy.kristpay.database;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import pw.lemmmy.kristpay.KristPay;
import pw.lemmmy.kristpay.economy.KristTransactionResult;
import pw.lemmmy.kristpay.economy.KristTransferResult;
import pw.lemmmy.kristpay.krist.KristTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class TransactionLogEntry {
	private boolean success;
	private String error;
	private String type;
	private String fromAccount;
	private String toAccount;
	private String fromAddress;
	private String destAddress;
	private String toAddress;
	private int amount = 0;
	private String returnAddress;
	private String metaMessage;
	private String metaError;
	private int kristTXID = -1;
	
	public TransactionLogEntry() {}
	
	public TransactionLogEntry setTransactionResult(TransactionResult transactionResult) {
		success = transactionResult.getResult().equals(ResultType.SUCCESS);
		error = success ? null : transactionResult.getResult().name().toLowerCase();
		type = transactionResult.getType().getName().toLowerCase();
		
		if (!success && transactionResult instanceof KristTransactionResult && ((KristTransactionResult) transactionResult).getError() != null) {
			error = ((KristTransactionResult) transactionResult).getError();
		}
		
		return this;
	}
	
	public TransactionLogEntry setTransferResult(TransferResult transferResult) {
		success = transferResult.getResult().equals(ResultType.SUCCESS);
		error = success ? null : transferResult.getResult().name().toLowerCase();
		type = transferResult.getType().getName().toLowerCase();
		
		if (!success && transferResult instanceof KristTransferResult && ((KristTransferResult) transferResult).getError() != null) {
			error = ((KristTransferResult) transferResult).getError();
		}
		
		return this;
	}
	
	public TransactionLogEntry setType(EntryType entryType) {
		type = entryType.name().toLowerCase();
		return this;
	}
	
	public TransactionLogEntry setFromAccount(UniqueAccount account) {
		fromAccount = account.getIdentifier();
		return this;
	}
	
	public TransactionLogEntry setToAccount(UniqueAccount account) {
		toAccount = account.getIdentifier();
		return this;
	}
	
	public TransactionLogEntry setTransaction(KristTransaction transaction) {
		kristTXID = transaction.getId();
		fromAddress = transaction.getFrom();
		toAddress = transaction.getTo();
		return this;
	}
	
	public TransactionLogEntry setMeta(Map<String, String> meta) {
		if (meta == null) return this;
		
		returnAddress = meta.get("return");
		metaMessage = meta.get("message");
		metaError = meta.get("error");
		
		return this;
	}
	
	public TransactionLogEntry addAsync() {
		Task.builder().execute(() -> {
			String query = "INSERT INTO tx_log (success, error, type, from_account, to_account, from_address, " +
				"dest_address, to_address, amount, return_address, meta_message, meta_error, krist_txid) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			try (Connection conn = KristPay.INSTANCE.getDatabase().getDataSource().getConnection();
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
		
		return this;
	}
	
	private static PreparedStatement prepareGetEntry(Connection conn, int id) throws SQLException {
		String query = "SELECT * FROM tx_log WHERE id = ?";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setInt(1, id);
		return stmt;
	}
	
	public static Optional<TransactionLogEntry> getEntry(DataSource data, int id) {
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = prepareGetEntry(conn, id);
			 ResultSet results = stmt.executeQuery()) {
			TransactionLogEntry entry = new TransactionLogEntry();
			
			entry.success = results.getBoolean("success");
			entry.error = results.getString("error");
			entry.type = results.getString("type");
			entry.fromAccount = results.getString("from_account");
			entry.toAccount = results.getString("to_account");
			entry.fromAddress = results.getString("from_address");
			entry.destAddress = results.getString("dest_address");
			entry.toAddress = results.getString("to_address");
			entry.amount = results.getInt("amount");
			entry.returnAddress = results.getString("return_address");
			entry.metaMessage = results.getString("meta_message");
			entry.metaError = results.getString("meta_error");
			entry.kristTXID = results.getInt("krist_txid");
			
			return Optional.of(entry);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return Optional.empty();
	}
	
	public enum EntryType {
		DEPOSIT, WITHDRAW, TRANSFER
	}
}

package pw.lemmmy.kristpay.database;

import lombok.Data;
import lombok.experimental.Accessors;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class FaucetReward {
	private int id;
	
	private String ip;
	private String account;
	private int value;
	private int rewardTier;
	private Timestamp time;
	private Timestamp minimum;
	private Timestamp expires;
	
	public FaucetReward() {}
	
	public FaucetReward setIP(InetSocketAddress address) {
		ip = address.getAddress().toString(); // save the address, not the port (port always changes)
		return this;
	}
	
	public FaucetReward setAccount(UniqueAccount account) {
		this.account = account.getIdentifier();
		return this;
	}
	
	public FaucetReward setTimestamps(long minimumTime, long expireTime) {
		long currentTime = System.currentTimeMillis();
		
		time = new Timestamp(currentTime);
		minimum = new Timestamp(currentTime + minimumTime);
		expires = new Timestamp(currentTime + expireTime);
		
		return this;
	}
	
	public FaucetReward add() {
		String query = "INSERT INTO faucet_rewards (ip, account, value, reward_tier, time, minimum, expires)" +
			"VALUES (?, ?, ?, ?, ?, ?, ?)";
		
		try (Connection conn = KristPay.INSTANCE.getDatabase().getDataSource().getConnection();
			 PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, ip);
			stmt.setString(2, account);
			stmt.setInt(3, value);
			stmt.setInt(4, rewardTier);
			stmt.setTimestamp(5, time);
			stmt.setTimestamp(6, minimum);
			stmt.setTimestamp(7, expires);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	private static FaucetReward populateReward(ResultSet results) throws SQLException {
		FaucetReward reward = new FaucetReward();
		
		reward.id = results.getInt("id");
		
		reward.ip = results.getString("ip");
		reward.account = results.getString("account");
		reward.value = results.getInt("value");
		reward.rewardTier = results.getInt("reward_tier");
		reward.time = results.getTimestamp("time");
		reward.minimum = results.getTimestamp("minimum");
		reward.expires = results.getTimestamp("expires");
		
		return reward;
	}
	
	private static PreparedStatement prepareGetLastReward(Connection conn, String ip, String account) throws SQLException {
		String query = "SELECT * FROM faucet_rewards WHERE ip = ? OR account = ? ORDER BY time DESC LIMIT 1";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setString(1, ip);
		stmt.setString(2, account);
		return stmt;
	}
	
	public static Optional<FaucetReward> getLastReward(DataSource data, InetSocketAddress address, UniqueAccount account) {
		return getLastReward(data, address.getAddress().toString(), account.getIdentifier());
	}
	
	public static Optional<FaucetReward> getLastReward(DataSource data, String ip, String account) {
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = prepareGetLastReward(conn, ip, account);
			 ResultSet results = stmt.executeQuery()) {
			if (!results.next()) return Optional.empty();
			return Optional.of(populateReward(results));
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error getting faucet reward", e);
		}
		
		return Optional.empty();
	}
}

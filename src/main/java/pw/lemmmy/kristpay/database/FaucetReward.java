package pw.lemmmy.kristpay.database;

import lombok.Data;
import lombok.experimental.Accessors;
import org.spongepowered.api.scheduler.Task;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class FaucetReward {
	private int id;
	
	private String ip;
	private String userID;
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
	
	public FaucetReward setUserID(UUID uuid) {
		userID = uuid.toString();
		return this;
	}
	
	public FaucetReward setTimestamps(long timeAway) {
		long currentTime = System.currentTimeMillis();
		
		time = new Timestamp(currentTime);
		minimum = new Timestamp(currentTime + timeAway);
		expires = new Timestamp(currentTime + (timeAway * 2));
		
		return this;
	}
	
	public FaucetReward addAsync() {
		Task.builder().execute(() -> {
			String query = "INSERT INTO faucet_rewards (ip, user_id, value, reward_tier, time, minimum, expires)" +
				"VALUES (?, ?, ?, ?, ?, ?, ?)";
			
			try (Connection conn = KristPay.INSTANCE.getDatabase().getDataSource().getConnection();
				 PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setString(1, ip);
				stmt.setString(2, userID);
				stmt.setInt(3, value);
				stmt.setInt(4, rewardTier);
				stmt.setTimestamp(5, time);
				stmt.setTimestamp(6, minimum);
				stmt.setTimestamp(7, expires);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}).async().name("KristPay - Faucet Reward").submit(KristPay.INSTANCE);
		
		return this;
	}
	
	private static FaucetReward populateReward(ResultSet results) throws SQLException {
		FaucetReward reward = new FaucetReward();
		
		reward.id = results.getInt("id");
		
		reward.ip = results.getString("ip");
		reward.userID = results.getString("user_id");
		reward.value = results.getInt("value");
		reward.rewardTier = results.getInt("reward_tier");
		reward.time = results.getTimestamp("time");
		reward.minimum = results.getTimestamp("minimum");
		reward.expires = results.getTimestamp("expires");
		
		return reward;
	}
	
	private static PreparedStatement prepareGetLastReward(Connection conn, String ip, String userID) throws SQLException {
		String query = "SELECT * FROM faucet_rewards WHERE ip = ? OR user_id = ? ORDER BY time DESC LIMIT 1";
		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setString(1, ip);
		stmt.setString(2, userID);
		return stmt;
	}
	
	public static Optional<FaucetReward> getLastReward(DataSource data, InetSocketAddress address, UUID uuid) {
		return getLastReward(data, address.getAddress().toString(), uuid.toString());
	}
	
	public static Optional<FaucetReward> getLastReward(DataSource data, String ip, String userID) {
		try (Connection conn = data.getConnection();
			 PreparedStatement stmt = prepareGetLastReward(conn, ip, userID);
			 ResultSet results = stmt.executeQuery()) {
			if (!results.next()) return Optional.empty();
			return Optional.of(populateReward(results));
		} catch (SQLException e) {
			KristPay.INSTANCE.getLogger().error("Error getting faucet reward", e);
		}
		
		return Optional.empty();
	}
}

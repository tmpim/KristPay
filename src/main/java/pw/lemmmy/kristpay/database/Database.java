package pw.lemmmy.kristpay.database;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Database {
	private static final String DB_URI = "jdbc:h2:kristpay.log.db"; // TODO: config opt?
	
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
	}
}

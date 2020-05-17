package pw.lemmmy.kristpay.database.migrations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implements the account table and changes the format of UUIDs.
 */
public class Migration_2020_05_17_204553_create_old_indexes extends Migration {
    @Override
    public void run(DataSource dataSource, Connection conn) throws SQLException {
        try (
            PreparedStatement stmt1 = conn.prepareStatement("CREATE INDEX tx_log__from_account ON tx_log(from_account)");
            PreparedStatement stmt2 = conn.prepareStatement("CREATE INDEX tx_log__to_account ON tx_log(to_account)");
            PreparedStatement stmt3 = conn.prepareStatement("CREATE INDEX faucet_rewards__account ON faucet_rewards(account)")
        ) {
            stmt1.executeUpdate();
            stmt2.executeUpdate();
            stmt3.executeUpdate();
        }
    }
}

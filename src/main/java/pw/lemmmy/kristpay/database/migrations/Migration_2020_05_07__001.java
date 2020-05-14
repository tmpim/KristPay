package pw.lemmmy.kristpay.database.migrations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The initial database schema, from the original implementation of transaction logging in 2018.
 */
public class Migration_2020_05_07__001 extends Migration {
    @Override
    public void run(DataSource dataSource, Connection conn) throws SQLException {
        String txLogTable = "CREATE TABLE IF NOT EXISTS tx_log (\n" +
        "    id INT NOT NULL AUTO_INCREMENT,\n" +
        "    success BOOLEAN,\n" +
        "    error VARCHAR(255),\n" +
        "    type VARCHAR(255),\n" +
        "    from_account VARCHAR(255),\n" +
        "    to_account VARCHAR(255),\n" +
        "    from_address VARCHAR(255),\n" +
        "    dest_address VARCHAR(255),\n" +
        "    to_address VARCHAR(255),\n" +
        "    amount INT,\n" +
        "    return_address VARCHAR(255),\n" +
        "    meta_message VARCHAR(255),\n" +
        "    meta_error VARCHAR(255),\n" +
        "    krist_txid INT,\n" +
        "    time TIMESTAMP,\n" +
        "    PRIMARY KEY (id)\n" +
        ")";
    
        String faucetRewardTable = "CREATE TABLE IF NOT EXISTS faucet_rewards (\n" +
        "    id INT NOT NULL AUTO_INCREMENT,\n" +
        "    ip VARCHAR(255),\n" +
        "    account VARCHAR(255),\n" +
        "    value INT,\n" +
        "    reward_tier INT,\n" +
        "    time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
        "    minimum TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
        "    expires TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
        "    PRIMARY KEY (id)\n" +
        ")";
    
        try (PreparedStatement stmtTxLogTable = conn.prepareStatement(txLogTable);
             PreparedStatement stmtFaucetRewardTable = conn.prepareStatement(faucetRewardTable)) {
            stmtTxLogTable.executeUpdate();
            stmtFaucetRewardTable.executeUpdate();
        }
    }
}

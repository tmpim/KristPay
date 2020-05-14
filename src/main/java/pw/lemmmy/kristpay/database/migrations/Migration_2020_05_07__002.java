package pw.lemmmy.kristpay.database.migrations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implements the account table, changes the format of UUIDs and creates indexes.
 */
public class Migration_2020_05_07__002 extends Migration {
    @Override
    public void run(DataSource dataSource, Connection conn) throws SQLException {
        String accountTable = "CREATE TABLE IF NOT EXISTS accounts (\n" +
        "    owner CHAR(36) PRIMARY KEY,\n" +
        "    deposit_address CHAR(10) NOT NULL,\n" +
        "    deposit_password VARCHAR(100) NOT NULL,\n" +
        "    balance INT NOT NULL,\n" +
        "    unseen_deposit INT NOT NULL,\n" +
        "    unseen_transfer INT NOT NULL,\n" +
        "    welfare_counter INT NOT NULL,\n" +
        "    welfare_amount INT NOT NULL DEFAULT -1,\n" +
        "    welfare_last_payment TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n" +
        ")";
    
        String alterTxLog = "ALTER TABLE tx_log\n" +
        "    MODIFY from_account CHAR(36),\n" +
        "    MODIFY to_account CHAR(36),\n" +
        "    MODIFY from_address CHAR(10),\n" +
        "    MODIFY to_address CHAR(10)";
    
        try (PreparedStatement stmtAccountTable = conn.prepareStatement(accountTable);
             PreparedStatement stmtAlterTxLog = conn.prepareStatement(alterTxLog)) {
            stmtAccountTable.executeUpdate();
            stmtAlterTxLog.executeUpdate();
        }
    }
}

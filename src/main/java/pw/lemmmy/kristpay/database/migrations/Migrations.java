package pw.lemmmy.kristpay.database.migrations;

import org.slf4j.Logger;
import pw.lemmmy.kristpay.KristPay;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Migrations {
    private static final List<Migration> MIGRATIONS = new ArrayList<Migration>() {{
        add(new Migration_2020_05_07_000001_create_tx_log_faucet_rewards_tables());
        add(new Migration_2020_05_07_000002_create_accounts_table());
        add(new Migration_2020_05_17_204553_create_old_indexes());
    }};
    
    private static void createMigrationsTable(Connection conn) throws SQLException {
        String migrationsTable = "CREATE TABLE IF NOT EXISTS migrations (\n" +
        "    migration VARCHAR(255) PRIMARY KEY,\n" +
        "    ran TINYINT\n" +
        ")";
        
        try (PreparedStatement stmtMigrationsTable = conn.prepareStatement(migrationsTable)) {
            stmtMigrationsTable.executeUpdate();
        }
    }
    
    public static void runMigrations(DataSource dataSource, Connection conn) throws SQLException {
        Logger log = KristPay.INSTANCE.getLogger();
        log.info("Running migrations...");
        
        createMigrationsTable(conn);
        
        // Figure out which migrations have already run
        Set<String> ranMigrations = new HashSet<>();
        try (
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM migrations WHERE ran = 1");
            ResultSet results = stmt.executeQuery()
        ) {
            while (results.next()) ranMigrations.add(results.getString("migration"));
        }
    
        for (Migration migration : MIGRATIONS) {
            String migrationName = migration.getClass().getSimpleName();
            if (ranMigrations.contains(migrationName)) {
                log.info("Migration {} already run.", migrationName);
                continue; // skip ones we've already run
            }
            
            log.info("Running migration {}", migrationName);
            migration.run(dataSource, conn);
            
            // mark it as ran
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO migrations (migration, ran) VALUES (?, 1)")) {
                stmt.setString(1, migrationName);
                stmt.executeUpdate();
            }
        }
        
        log.info("Migrations complete");
    }
}

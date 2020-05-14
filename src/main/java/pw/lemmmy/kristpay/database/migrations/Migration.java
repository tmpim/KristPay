package pw.lemmmy.kristpay.database.migrations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class Migration {
    public abstract void run(DataSource dataSource, Connection conn) throws SQLException;
}

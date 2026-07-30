package cl.go2.b2bi.jmc.foliador.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseFactory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFactory.class);
    private static HikariDataSource dataSource;

    private DatabaseFactory() {}

    public static synchronized void initialize() {
        if (dataSource == null || dataSource.isClosed()) {
            try {
                Properties props = new Properties();
                try (InputStream is = DatabaseFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
                    if (is != null) {
                        props.load(is);
                    } else {
                        // Valores por defecto
                        props.setProperty("db.url", System.getProperty("DB_URL", "jdbc:oracle:thin:@localhost:1521/XEPDB1"));
                        props.setProperty("db.user", System.getProperty("DB_USER", "usuario"));
                        props.setProperty("db.password", System.getProperty("DB_PASS", "password"));
                    }
                }

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(props.getProperty("db.url"));
                config.setUsername(props.getProperty("db.user"));
                config.setPassword(props.getProperty("db.password"));
                config.setDriverClassName(props.getProperty("db.driver", "oracle.jdbc.OracleDriver"));

                // Configuración de performance multihilo
                config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "20")));
                config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
                config.setIdleTimeout(30000);
                config.setConnectionTimeout(10000);
                config.setPoolName("AxwayFoliadorHikariPool");

                dataSource = new HikariDataSource(config);
                logger.info("DatabaseFactory: Pool de conexiones HikariCP inicializado con éxito.");
            } catch (Exception e) {
                logger.error("DatabaseFactory: Error inicializando el DataSource", e);
                throw new RuntimeException("Error iniciando pool de base de datos", e);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("DatabaseFactory: Pool de conexiones cerrado.");
        }
    }
}
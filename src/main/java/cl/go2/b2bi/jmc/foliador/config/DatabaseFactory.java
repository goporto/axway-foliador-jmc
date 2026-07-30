package cl.go2.b2bi.jmc.foliador.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseFactory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFactory.class);
    private static HikariDataSource dataSource;

    private DatabaseFactory() {}

    public static synchronized void initialize() throws IOException {
        if (dataSource == null || dataSource.isClosed()) {

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
        }

    }

    public static synchronized void initialize(String configFilePath) throws IOException {
        if (dataSource == null || dataSource.isClosed()) {

            Properties props = new Properties();
            // Cargar dinámicamente las propiedades del disco
            try (InputStream fis = new FileInputStream(configFilePath)) {
                props.load(fis);
            }

            initialize_pool(props);
        }

    }


    public static synchronized void initialize_pool( Properties props) {
        if (dataSource == null || dataSource.isClosed()) {
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(props.getProperty("db.url"));
                config.setUsername(props.getProperty("db.user"));
                config.setPassword(props.getProperty("db.password"));
                config.setDriverClassName(props.getProperty("db.driver", "oracle.jdbc.OracleDriver"));

                // Configuración de performance multihilo
                config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "20")));
                config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
                config.setIdleTimeout(Integer.parseInt(props.getProperty("db.pool.idleTimeout", "30000")));
                config.setConnectionTimeout(Integer.parseInt(props.getProperty("db.pool.connectionTimeout", "10000")));
                config.setPoolName("GO2FoliadorPool");

                dataSource = new HikariDataSource(config);
                logger.info("DatabaseFactory: Pool de conexiones GO2FoliadorPool inicializado con éxito.");
            } catch (Exception e) {
                logger.error("DatabaseFactory: Error inicializando el DataSource", e);
                throw new RuntimeException("Error iniciando pool de base de datos", e);
            }
        }
    }

    public static Connection getConnection() throws SQLException, IOException {
        if (dataSource == null) {
            initialize();
        }
        return dataSource.getConnection();
    }

    public static Connection getConnection(String configFilePath) throws SQLException, IOException {
        if (dataSource == null) {
            initialize(configFilePath);
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
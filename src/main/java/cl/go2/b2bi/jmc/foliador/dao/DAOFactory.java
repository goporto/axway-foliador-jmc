package cl.go2.b2bi.jmc.foliador.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOFactory {

    private static final Logger logger = LoggerFactory.getLogger(DAOFactory.class);
    private static FoliadorDAO instance;

    public static synchronized FoliadorDAO getDAO(String dbDriverOrUrl) {
        if (instance == null) {
            if (dbDriverOrUrl != null && dbDriverOrUrl.toLowerCase().contains("postgresql")) {
                logger.info("DAOFactory: Detectado entorno POSTGRESQL. Instanciando FoliadorPostgresDAO.");
                instance = new FoliadorPostgresDAO();
            } else {
                logger.info("DAOFactory: Detectado entorno ORACLE. Instanciando FoliadorOracleDAO.");
                instance = new FoliadorOracleDAO();
            }
        }
        return instance;
    }
}
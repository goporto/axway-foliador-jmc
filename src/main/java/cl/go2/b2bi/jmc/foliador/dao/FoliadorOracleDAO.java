package cl.go2.b2bi.jmc.foliador.dao;

import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cl.go2.b2bi.jmc.foliador.model.PositionConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FoliadorOracleDAO implements FoliadorDAO {

    private static final Logger logger = LoggerFactory.getLogger(FoliadorOracleDAO.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getCorrelativoValTraza(Connection conn) throws SQLException {
        String sql = "SELECT CORRELATIVO_VAL_TRAZA.NEXTVAL FROM DUAL";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("%08d", rs.getLong(1));
            }
        }
        throw new SQLException("[Oracle] No se pudo obtener CORRELATIVO_VAL_TRAZA.NEXTVAL");
    }

    @Override
    public String[] obtenerCodInstYCasilla(Connection conn, String usuarioCasilla) throws SQLException {
        String[] res = executeProcCodInstYCasilla(conn, usuarioCasilla);
        if (res == null) {
            res = executeProcCodInstYCasilla(conn, "NE");
        }
        return res;
    }

    private String[] executeProcCodInstYCasilla(Connection conn, String casilla) throws SQLException {
        String sql = "{call PROC_OBTENER_COD_INST_Y_CASILLA_F1(?, ?)}";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.setString(1, casilla);
            cstmt.registerOutParameter(2, OracleTypes.CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                if (rs != null && rs.next()) {
                    return new String[]{rs.getString(1), rs.getString(2)};
                }
            }
        }
        return null;
    }

    @Override
    public String obtenerTDoc(Connection conn, String tDocHeader) throws SQLException {
        String res = executeProcTDoc(conn, tDocHeader);
        if (res == null) {
            res = executeProcTDoc(conn, "000");
        }
        return res;
    }

    private String executeProcTDoc(Connection conn, String header) throws SQLException {
        String sql = "{call PROC_OBTENER_T_DOC(?, ?)}";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.setString(1, header);
            cstmt.registerOutParameter(2, OracleTypes.CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                if (rs != null && rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    @Override
    public String getCorrelativoEntidad(Connection conn, String entidadOrigen) throws SQLException {
        String sql = "SELECT CORRELATIVO_ENTRADA_" + entidadOrigen + ".NEXTVAL FROM DUAL";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("%08d", rs.getLong(1));
            }
        }
        return "00000000";
    }

    @Override
    public int obtenerVersionMaxMsg(Connection conn, String tDoc) throws SQLException {
        String sql = "{call PROC_OBTENER_VERSION_MAX_MSG(?, ?)}";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.setString(1, tDoc);
            cstmt.registerOutParameter(2, OracleTypes.CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                if (rs != null && rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    @Override
    public void insertarEncabezadoTraza(Connection conn, String correlativo, String flujo,
                                        String usuarioCasilla, String entidadOrigen,
                                        String archivoEntrada, String tDoc, long fileSize,
                                        String correlativoInst, int version, long cantLineas,
                                        String filial, String fecha) throws SQLException {
        String sql = "{? = call FUNC_VAL_INSERTAR_ENCABEZADO_TRAZA(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.INTEGER);
            cstmt.setString(2, correlativo);
            cstmt.setString(3, flujo);
            cstmt.setString(4, usuarioCasilla);
            cstmt.setString(5, entidadOrigen);
            cstmt.setString(6, archivoEntrada);
            cstmt.setString(7, tDoc);
            cstmt.setLong(8, fileSize);
            cstmt.setString(9, correlativoInst);
            cstmt.setInt(10, version);
            cstmt.setLong(11, cantLineas);
            cstmt.setString(12, filial);
            cstmt.setString(13, fecha);
            cstmt.execute();
        }
    }

    @Override
    public void setMovement(Connection conn, String correlativo, String estado) throws SQLException {
        String formattedDate = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "{? = call FUNC_VAL_CARGAMOVIMIENTO(?, ?, ?, ?, ?, ?)}";

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            // Parámetro 1: Valor de retorno (str)
            cstmt.registerOutParameter(1, Types.VARCHAR);

            // Parámetros de entrada
            cstmt.setString(2, correlativo);              // 1. correlativomov (VARCHAR)
            cstmt.setNull(3, Types.VARCHAR);             // 2. nombrearchsalida (VARCHAR)
            cstmt.setNull(4, Types.INTEGER);             // 3. cantreg (NUMBER/INTEGER)
            cstmt.setNull(5, Types.VARCHAR);             // 4. casdestino (VARCHAR)
            cstmt.setString(6, estado);                  // 5. estadomovaux (VARCHAR)
            cstmt.setString(7, formattedDate);           // 6. fechamov (VARCHAR)

            cstmt.execute();

            String result = cstmt.getString(1);
            logger.info("[Oracle] FUNC_VAL_CARGAMOVIMIENTO ejecutada para correlativo {} con estado {}. Resultado: {}",
                    correlativo, estado, result);

        } catch (SQLException e) {
            logger.error("[Oracle] Error al ejecutar FUNC_VAL_CARGAMOVIMIENTO para correlativo " + correlativo, e);
            throw e;
        }
    }
    @Override
    public void rejectAndSaveError(Connection conn, String correlativo, String codError, String msg) throws SQLException {
        String formattedDate = LocalDateTime.now().format(DATE_FORMATTER);
        String sqlFunc = "{? = call FUNC_VAL_CARGAERROR(?, ?, ?, ?, ?)}";

        try (CallableStatement cstmt = conn.prepareCall(sqlFunc)) {
            cstmt.registerOutParameter(1, Types.VARCHAR);
            cstmt.setString(2, correlativo);
            cstmt.setString(3, codError);
            cstmt.setString(4, msg);
            cstmt.setString(5, formattedDate);
            cstmt.setString(6, "1");

            cstmt.execute();

            String result = cstmt.getString(1);
            logger.info("[Oracle] FUNC_VAL_CARGAERROR ejecutada para correlativo {}. Resultado: {}", correlativo, result);
        } catch (SQLException e) {
            logger.error("[Oracle] Error al invocar FUNC_VAL_CARGAERROR para correlativo " + correlativo, e);
            throw e;
        }

        this.setMovement(conn, correlativo, "000");
    }

    @Override
    public int obtenerLargoRegistro(Connection conn, String tDoc, String modo) throws SQLException {
        String sql = "{? = call FUNC_BUSCAR_LARGO_REGISTRO(?, ?)}";
        String modoParam = (modo == null || modo.isEmpty()) ? "F" : modo;

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.INTEGER);
            cstmt.setString(2, tDoc);
            cstmt.setString(3, modoParam);
            cstmt.execute();
            return cstmt.getInt(1);
        } catch (SQLException e) {
            logger.error("[Oracle] Error al consultar FUNC_BUSCAR_LARGO_REGISTRO para tDoc: " + tDoc, e);
            return 322;
        }
    }

    @Override
    public List<String> obtenerListaTDocs(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();

        // Sintaxis estándar JDBC para PROCEDURE con 1 parámetro OUT refcursor en Oracle:
        String sql = "{ call proc_obtener_lista_tdocs(?) }";

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            // En Oracle usas Types.REF_CURSOR (o oracle.jdbc.OracleTypes.CURSOR)
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            cstmt.execute();

            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                while (rs != null && rs.next()) {
                    list.add(rs.getString(1));
                }
            }
            logger.info("[Oracle] proc_obtener_lista_tdocs ejecutada exitosamente. Total registros: {}", list.size());
        } catch (SQLException e) {
            logger.error("[Oracle] Error al ejecutar proc_obtener_lista_tdocs", e);
            throw e;
        }

        return list;
    }


    @Override
    public List<PositionConfig> obtenerPosiciones(Connection conn) throws SQLException {
        List<PositionConfig> list = new ArrayList<>();
        String sql = "{call PROC_OBTENER_POSICIONES(?)}";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, OracleTypes.CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                while (rs != null && rs.next()) {
                    list.add(new PositionConfig(
                            rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)
                    ));
                }
            }
        }
        return list;
    }
}

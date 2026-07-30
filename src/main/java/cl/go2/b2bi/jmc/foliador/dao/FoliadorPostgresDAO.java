package cl.go2.b2bi.jmc.foliador.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cl.go2.b2bi.jmc.foliador.model.PositionConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FoliadorPostgresDAO implements FoliadorDAO {

    private static final Logger logger = LoggerFactory.getLogger(FoliadorPostgresDAO.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getCorrelativoValTraza(Connection conn) throws SQLException {
        String sql = "SELECT nextval('correlativo_val_traza_seq')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("%08d", rs.getLong(1));
            }
        }
        throw new SQLException("[PostgreSQL] No se pudo obtener nextval('correlativo_val_traza_seq')");
    }

    @Override
    public String[] obtenerCodInstYCasilla(Connection conn, String usuarioCasilla) throws SQLException {
        String[] res = executeFuncCodInstYCasilla(conn, usuarioCasilla);
        if (res == null) {
            res = executeFuncCodInstYCasilla(conn, "NE");
        }
        return res;
    }

    private String[] executeFuncCodInstYCasilla(Connection conn, String casilla) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        if (originalAutoCommit) {
            conn.setAutoCommit(false);
        }

        String sql = "{ ? = call rdc.proc_obtener_cod_inst_y_casilla_f1(?) }";

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            cstmt.setString(2, casilla);
            cstmt.execute();

            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                if (rs != null && rs.next()) {
                    String codInst = rs.getString(1);
                    String userCasilla = rs.getString(2);
                    return new String[]{codInst, userCasilla};
                }
            }
        } finally {
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return null;
    }

    @Override
    public String obtenerTDoc(Connection conn, String tDocHeader) throws SQLException {
        String res = executeFuncTDoc(conn, tDocHeader);
        if (res == null) {
            res = executeFuncTDoc(conn, "000");
        }
        return res;
    }

    private String executeFuncTDoc(Connection conn, String header) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            // En PostgreSQL, leer un OUT refcursor requiere estar dentro de una transacción
            if (!conn.getAutoCommit()) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }

            conn.setAutoCommit(false);

            // Sintaxis correcta para PROCEDURE con IN (pos 1) y OUT (pos 2):
            String sql = "call rdc.proc_obtener_t_doc(?, ?)";

            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                // Parámetro 1: IN p_t_doc (character varying)
                cstmt.setString(1, header);

                // Parámetro 2: OUT p_result (refcursor)
                cstmt.registerOutParameter(2, Types.REF_CURSOR);

                // Ejecución del Stored Procedure
                cstmt.execute();

                // Recuperar el ResultSet desde el parámetro OUT posicional 2
                try (ResultSet rs = (ResultSet) cstmt.getObject(2)) {
                    if (rs != null && rs.next()) {
                        String tDocResult = rs.getString(1); // O rs.getString("t_doc")
                        conn.commit();
                        return tDocResult;
                    }
                }
            }

            conn.commit();

        } catch (SQLException e) {
            if (!conn.getAutoCommit()) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("[PostgreSQL] Error al hacer rollback", rollbackEx);
                }
            }
            logger.error("[PostgreSQL] Error al ejecutar proc_obtener_t_doc para header: " + header, e);
            throw e;
        } finally {
            if (originalAutoCommit) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {}
            }
        }
        return null;
    }

    @Override
    public String getCorrelativoEntidad(Connection conn, String entidadOrigen) throws SQLException {
        String sql = "SELECT nextval('correlativo_entrada_" + entidadOrigen.toLowerCase() + "_seq')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return String.format("%08d", rs.getLong(1));
            }
        } catch (SQLException e) {
            logger.warn("[PostgreSQL] Secuencia para entidad {} no encontrada. Usando valor por defecto.", entidadOrigen);
        }
        return "00000000";
    }

    @Override
    public int obtenerVersionMaxMsg(Connection conn, String tDoc) throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        if (originalAutoCommit) {
            conn.setAutoCommit(false);
        }

        String sql = "{ ? = call rdc.proc_obtener_version_max_msg(?) }";

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            cstmt.setString(2, tDoc);
            cstmt.execute();

            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                if (rs != null && rs.next()) {
                    return rs.getInt(1);
                }
            }
        } finally {
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
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
        String sql = "{ ? = call rdc.func_val_insertar_encabezado_traza(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }";
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

        // Invocación explícita pasando null::varchar para evitar ambigüedades en Postgres
        String sql = "SELECT rdc.func_val_cargamovimiento(?::varchar, null::varchar, null::integer, null::varchar, ?::varchar, ?::varchar)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativo);
            pstmt.setString(2, estado);
            pstmt.setString(3, formattedDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String result = rs.getString(1);
                    logger.info("[PostgreSQL] func_val_cargamovimiento ejecutada para correlativo {} con estado {}. Resultado: {}",
                            correlativo, estado, result);
                }
            }
        } catch (SQLException e) {
            logger.error("[PostgreSQL] Error al ejecutar func_val_cargamovimiento para correlativo " + correlativo, e);
            throw e;
        }
    }

    @Override
    public void rejectAndSaveError(Connection conn, String correlativo, String codError, String msg) throws SQLException {
        String formattedDate = LocalDateTime.now().format(DATE_FORMATTER);

        // 1. OBLIGATORIO EN POSTGRESQL:
        // Si la conexión viene abortada por un error previo (ej. fallo al insertar encabezado),
        // limpiamos el estado con rollback para poder registrar el error exitosamente.
        if (!conn.getAutoCommit()) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                logger.warn("[PostgreSQL] No se pudo hacer rollback preventivo: {}", rollbackEx.getMessage());
            }
        }

        // 2. Invocar la función con SELECT directo
        String sql = "SELECT rdc.func_val_cargaerror(?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativo);
            pstmt.setString(2, codError);
            pstmt.setString(3, msg);
            pstmt.setString(4, formattedDate);
            pstmt.setString(5, "1");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String result = rs.getString(1);
                    logger.info("[PostgreSQL] func_val_cargaerror ejecutada para correlativo {}. Respuesta: {}", correlativo, result);
                }
            }

            // Si no está en autoCommit, confirmamos la inserción del error
            if (!conn.getAutoCommit()) {
                conn.commit();
            }

        } catch (SQLException e) {
            if (!conn.getAutoCommit()) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            logger.error("[PostgreSQL] Error al invocar func_val_cargaerror para correlativo " + correlativo, e);
            throw e;
        }

        // 3. Cambiar estado a movimiento '000'
        this.setMovement(conn, correlativo, "000");
    }

    @Override
    public int obtenerLargoRegistro(Connection conn, String tDoc, String modo) throws SQLException {
        String sql = "{ ? = call rdc.func_buscar_largo_registro(?, ?) }";
        String modoParam = (modo == null || modo.isEmpty()) ? "F" : modo;

        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.INTEGER);
            cstmt.setString(2, tDoc);
            cstmt.setString(3, modoParam);
            cstmt.execute();
            return cstmt.getInt(1);
        } catch (SQLException e) {
            logger.error("[PostgreSQL] Error al consultar func_buscar_largo_registro para tDoc: " + tDoc, e);
        }
        return 322;
    }

    @Override
    public List<String> obtenerListaTDocs(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        boolean originalAutoCommit = conn.getAutoCommit();
        if (originalAutoCommit) {
            conn.setAutoCommit(false);
        }

        String sql = "{ ? = call rdc.proc_obtener_lista_tdocs() }";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                while (rs != null && rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        } finally {
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return list;
    }

    @Override
    public List<PositionConfig> obtenerPosiciones(Connection conn) throws SQLException {
        List<PositionConfig> list = new ArrayList<>();
        boolean originalAutoCommit = conn.getAutoCommit();
        if (originalAutoCommit) {
            conn.setAutoCommit(false);
        }

        String sql = "{ ? = call rdc.proc_obtener_posiciones() }";
        try (CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.registerOutParameter(1, Types.REF_CURSOR);
            cstmt.execute();
            try (ResultSet rs = (ResultSet) cstmt.getObject(1)) {
                while (rs != null && rs.next()) {
                    list.add(new PositionConfig(
                            rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)
                    ));
                }
            }
        } finally {
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return list;
    }
}
package cl.go2.b2bi.jmc.foliador.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cl.go2.b2bi.jmc.foliador.model.PositionConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FoliadorPostgresDAO implements FoliadorDAO {

    private static final Logger logger = LoggerFactory.getLogger(FoliadorPostgresDAO.class);

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
        String sql = "SELECT * FROM proc_obtener_cod_inst_y_casilla_f1(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, casilla);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString(1), rs.getString(2)};
                }
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
        String sql = "SELECT * FROM proc_obtener_t_doc(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, header);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
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
        String sql = "SELECT * FROM proc_obtener_version_max_msg(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tDoc);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
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
        String sql = "SELECT func_val_insertar_encabezado_traza(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativo);
            pstmt.setString(2, flujo);
            pstmt.setString(3, usuarioCasilla);
            pstmt.setString(4, entidadOrigen);
            pstmt.setString(5, archivoEntrada);
            pstmt.setString(6, tDoc);
            pstmt.setLong(7, fileSize);
            pstmt.setString(8, correlativoInst);
            pstmt.setInt(9, version);
            pstmt.setLong(10, cantLineas);
            pstmt.setString(11, filial);
            pstmt.setString(12, fecha);
            pstmt.execute();
        }
    }

    @Override
    public void setMovement(Connection conn, String correlativo, String estado) throws SQLException {
        String sql = "CALL proc_set_movement(?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativo);
            pstmt.setString(2, estado);
            pstmt.execute();
        } catch (SQLException e) {
            logger.warn("[PostgreSQL] Error al ejecutar proc_set_movement: {}", e.getMessage());
        }
    }

    @Override
    public void rejectAndSaveError(Connection conn, String correlativo, String codError, String msg, String fileContent) {
        String sql = "CALL proc_reject_and_save_error(?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correlativo);
            pstmt.setString(2, codError);
            pstmt.setString(3, msg);
            pstmt.setString(4, fileContent);
            pstmt.execute();
        } catch (SQLException e) {
            logger.error("[PostgreSQL] Error al registrar rechazo para correlativo " + correlativo, e);
        }
    }
    @Override
    public int obtenerLargoRegistro(Connection conn, String tDoc, String modo) throws SQLException {
        String sql = "SELECT func_buscar_largo_registro(?, ?)";
        String modoParam = (modo == null || modo.isEmpty()) ? "F" : modo;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tDoc);
            pstmt.setString(2, modoParam);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("[PostgreSQL] Error al consultar func_buscar_largo_registro para tDoc: " + tDoc, e);
        }
        return 322; // Valor de contingencia en DEV
    }
    @Override
    public List<String> obtenerListaTDocs(Connection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT * FROM proc_obtener_lista_tdocs()";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        }
        return list;
    }

    @Override
    public List<PositionConfig> obtenerPosiciones(Connection conn) throws SQLException {
        List<PositionConfig> list = new ArrayList<>();
        String sql = "SELECT * FROM proc_obtener_posiciones()";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new PositionConfig(
                        rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)
                ));
            }
        }
        return list;
    }
}
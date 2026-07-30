package cl.go2.b2bi.jmc.foliador.dao;

import cl.go2.b2bi.jmc.foliador.model.PositionConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface FoliadorDAO {
    String getCorrelativoValTraza(Connection conn) throws SQLException;
    String[] obtenerCodInstYCasilla(Connection conn, String usuarioCasilla) throws SQLException;
    String obtenerTDoc(Connection conn, String tDocHeader) throws SQLException;
    String getCorrelativoEntidad(Connection conn, String entidadOrigen) throws SQLException;
    int obtenerVersionMaxMsg(Connection conn, String tDoc) throws SQLException;
    void insertarEncabezadoTraza(Connection conn, String correlativo, String flujo,
                                 String usuarioCasilla, String entidadOrigen,
                                 String archivoEntrada, String tDoc, long fileSize,
                                 String correlativoInst, int version, long cantLineas,
                                 String filial, String fecha) throws SQLException;
    void setMovement(Connection conn, String correlativo, String estado) throws SQLException;
    void rejectAndSaveError(Connection conn, String correlativo, String codError, String msg)throws SQLException;
    /**
     * Consulta el largo del registro para un t_doc específico.
     * @param conn Conexión activa a BD
     * @param tDoc Tipo de documento (ej. 'RDC01')
     * @param modo Parámetro opcional (Default: 'F')
     * @return Largo en bytes del registro lógico
     */
    int obtenerLargoRegistro(Connection conn, String tDoc, String modo) throws SQLException;
    List<String> obtenerListaTDocs(Connection conn) throws SQLException;
    List<PositionConfig> obtenerPosiciones(Connection conn) throws SQLException;

}
package cl.go2.b2bi.jmc.foliador.service;

import cl.go2.b2bi.jmc.foliador.config.DatabaseFactory;
import cl.go2.b2bi.jmc.foliador.dao.DAOFactory;
import cl.go2.b2bi.jmc.foliador.dao.FoliadorDAO;
import cl.go2.b2bi.jmc.foliador.model.FoliadorResultDTO;
import com.axway.xib.Context;
import com.axway.xib.ProcessingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.concurrent.*;

public class FoliadorService {

    private static final Logger logger = LoggerFactory.getLogger(FoliadorService.class);
    //private final FoliadorDAO dao = new FoliadorDAO();
    private final HeaderParserService headerParser = new HeaderParserService();




    public FoliadorResultDTO executeFoliadoLogic(String usuarioCasillaInput, String archivoEntrada,
                                                 Context context, ProcessingMessage processingMessage,
                                                 long fileSize, long cantLineas) throws Exception {

        try (Connection conn = DatabaseFactory.getConnection()) {
            String dbUrl = conn.getMetaData().getURL();
            FoliadorDAO dao = DAOFactory.getDAO(dbUrl);
            conn.setAutoCommit(false); // Manejo transaccional

            // 1. Obtener correlativo principal
            String correlativo = dao.getCorrelativoValTraza(conn);
            logger.info("{}: Se consumió el correlativo \"{}\"", archivoEntrada, correlativo);

            String flujo = "VA1";

            // 2. Obtener código de institución y casilla
            String[] instCasilla = dao.obtenerCodInstYCasilla(conn, usuarioCasillaInput);
            String entidadOrigen = instCasilla[0];
            String usuarioCasilla = instCasilla[1];

            // 3. Parsear Header
            HeaderParserService.HeaderData headerData = headerParser.parseHeaderFromFile(context,processingMessage,conn,dao);

            String filial = headerData.institucionOFilial.equals(entidadOrigen) ? "0000" : headerData.institucionOFilial;

            // 4. Obtener t_doc
            String tDoc = dao.obtenerTDoc(conn, headerData.tDocHeader);

            // 5. Correlativo de la institución
            String correlativoInst = "00000000";
            if (!"NE".equals(usuarioCasilla)) {
                correlativoInst = dao.getCorrelativoEntidad(conn, entidadOrigen);
            }

            // 6. Obtener versión máxima
            int version = 1;
            if (!"000".equals(tDoc)) {
                version = dao.obtenerVersionMaxMsg(conn, tDoc);
            }

            // 7. Insertar encabezado traza
            dao.insertarEncabezadoTraza(conn, correlativo, flujo, usuarioCasilla, entidadOrigen,
                    archivoEntrada, tDoc, fileSize, correlativoInst, version, cantLineas, filial, headerData.fecha);

            dao.setMovement(conn, correlativo, "100");

            // 8. Validaciones de negocio y de errores
            if (fileSize == 0) {
                dao.rejectAndSaveError(conn, correlativo, "088", "Archivo vacío", archivoEntrada);
                conn.commit();
                throw new IllegalArgumentException("El archivo se encuentra vacío");
            }

            if (headerData.tDocHeader == null || headerData.tDocHeader.isEmpty()) {
                dao.rejectAndSaveError(conn, correlativo, "089", "Archivo de datos", archivoEntrada);
                conn.commit();
                throw new IllegalArgumentException("Línea de header vacía");
            }

            if ("NE".equals(usuarioCasilla)) {
                dao.rejectAndSaveError(conn, correlativo, "007", "Casilla no existe: " + usuarioCasillaInput, archivoEntrada);
                conn.commit();
                throw new IllegalArgumentException("Casilla no existe: " + usuarioCasillaInput);
            }

            if ("000".equals(tDoc)) {
                String codErr = headerData.encodeError ? "092" : "083";
                String msgErr = headerData.encodeError ? "Error de codificación" : "Documento no existe: " + headerData.tDocHeader;
                dao.rejectAndSaveError(conn, correlativo, codErr, msgErr, archivoEntrada);
                conn.commit();
                throw new IllegalArgumentException(msgErr);
            }

            conn.commit(); // Confirmación de transacción

            FoliadorResultDTO result = new FoliadorResultDTO(correlativo, tDoc, entidadOrigen, filial);
            result.setUsuarioCasilla(usuarioCasilla);
            result.setArchivoEntrada(archivoEntrada);
            return result;

        } catch (Exception e) {
            logger.error("Rollback ejecutado en foliador debido a error: " + e.getMessage());
            throw e;
        }
    }
}
package cl.go2.b2bi.jmc.foliador.service;

import cl.go2.b2bi.jmc.foliador.dao.FoliadorDAO;
import cl.go2.b2bi.jmc.foliador.model.PositionConfig;
import cl.go2.utils.HeaderReaderUtils;
import com.axway.xib.Context;
import com.axway.xib.Data;
import com.axway.xib.ProcessingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.util.List;

public class HeaderParserService {

    private static final Logger logger = LoggerFactory.getLogger(HeaderParserService.class);

    public static class HeaderData {
        public String tDocHeader;
        public String institucionOFilial;
        public String fecha;
        private int largoRegistro;

        public boolean encodeError = false;

        public HeaderData(String tDocHeader, String institucionOFilial, String fecha, int largoRegistro) {
            this.tDocHeader = tDocHeader;
            this.institucionOFilial = institucionOFilial;
            this.fecha = fecha;
            setLargoRegistro(largoRegistro);
        }

        public int getLargoRegistro() {
            return largoRegistro;
        }

        public void setLargoRegistro(int largoRegistro) {
            this.largoRegistro = largoRegistro;
        }
    }

    /**
     * Parsea la primera línea del archivo de forma dinámica utilizando la configuración de la BD.
     * Mantiene un consumo en RAM de $O(1)$ seguro para archivos de más de 2 GB.
     */
    public HeaderData parseHeaderFromFile(Context context, ProcessingMessage processingMessage,
                                          Connection conn, FoliadorDAO dao) {
        Data inputData = processingMessage.getData();
        InputStream in = inputData.getInput();
        if (in == null ) {
            HeaderData err = new HeaderData("000", "0000", null, 0);
            err.encodeError = true;
            return err;
        }

        String firstLine = HeaderReaderUtils.readFirstRecord(context,processingMessage);

        try {
            // 1. Obtener la lista de T_DOCS válidos y la matriz de posiciones desde la BD
            List<String> tDocs = dao.obtenerListaTDocs(conn);
            List<PositionConfig> positions = dao.obtenerPosiciones(conn);

            // 2. Evaluar cada regla de posición
            for (PositionConfig pos : positions) {
                // Conversión de 1-Based (SQL/Python) a 0-Based (Java substring)
                int startPos = pos.startPos - 1;
                int endPos = pos.endPos;

                int startInst = pos.startPosInstitucion - 1;
                int endInst = pos.endPosInstitucion;

                int startDate = pos.startPosDate - 1;
                int endDate = pos.endPosDate;

                // Extraer T_DOC candidato respetando los límites de la línea
                String tDocCandidate = safeSubstring(firstLine, startPos, endPos);

                if (tDocs.contains(tDocCandidate)) {
                    // Extraer Institución candidata y remover espacios
                    String instCandidate = safeSubstring(firstLine, startInst, endInst).replace(" ", "");

                    // Extraer Fecha candidata
                    String fechaCandidate = safeSubstring(firstLine, startDate, endDate).trim();

                    // Normalización de Institución: "0" + candidato
                    instCandidate = "0" + instCandidate;

                    String institucionFinal;
                    if (instCandidate.endsWith("0000")) {
                        institucionFinal = "-1";
                    } else {
                        int len = instCandidate.length();
                        institucionFinal = len >= 4 ? instCandidate.substring(len - 4) : instCandidate;
                    }

                    logger.info("Header parser dinámico exitoso: tDoc={}, institucion={}, fecha={}",
                            tDocCandidate, institucionFinal, fechaCandidate);

                    // obtenido el tipo de documento, obtenemos el largo de éste desde la BD,
                    int largo  = dao.obtenerLargoRegistro(conn, tDocCandidate,"F");
                    return new HeaderData(tDocCandidate, institucionFinal, fechaCandidate, largo);
                }
            }

        } catch (Exception e) {
            logger.error("Error consultando o aplicando reglas de posiciones dinámicas en la BD", e);
            HeaderData err = new HeaderData("000", "0000", null, 0);
            err.encodeError = true;
            return err;
        }

        // Si ninguna regla posicional coincidió
        return new HeaderData("000", "0000", null, 0);
    }

    /**
     * Helper equivalente a los Slices resilientes de Python (linea[start:end])
     * Evita lanzamientos de StringIndexOutOfBoundsException si la línea es más corta que lo esperado.
     */
    private String safeSubstring(String text, int start, int end) {
        if (text == null || start < 0 || start >= text.length()) {
            return "";
        }
        if (end > text.length()) {
            return text.substring(start);
        }
        return text.substring(start, end);
    }
}

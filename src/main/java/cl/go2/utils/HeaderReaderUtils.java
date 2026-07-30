package cl.go2.utils;
import com.axway.xib.Context;
import com.axway.xib.ProcessingMessage;
import com.axway.xib.Data;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HeaderReaderUtils {

    /**
     * Lee únicamente el primer registro del archivo gigante sin cargar el resto.
     */
    public static String readFirstRecord(Context context, ProcessingMessage message) {
        Data inputData = null;
        InputStream in = null;
        BufferedReader reader = null;

        try {
            // 1. Obtener acceso al flujo de datos del mensaje sin cargarlo en RAM [2, 10]
            inputData = message.getData();
            in = inputData.getInput();

            // 2. Recuperar el formato y longitud de registro de los metadatos [8, 9]
            String recordFormat = (String) message.getAttribute("LocalFileRecordFormat");
            if (recordFormat == null) {
                recordFormat = "FIXED_BINARY"; // Valor por defecto seguro [11, 12]
            }

            Object recLenObj = message.getAttribute("LocalFileRecordLength");
            int recordLength = 0;
            if (recLenObj instanceof Number) {
                recordLength = ((Number) recLenObj).intValue();
            } else if (recLenObj instanceof String) {
                recordLength = Integer.parseInt((String) recLenObj);
            }

            // 3. Estrategia de lectura según el formato de B2Bi [8, 9]
            if ("FIXED_BINARY".equalsIgnoreCase(recordFormat)) {
                if (recordLength <= 0) {
                    context.getContainer().getLogger().error("Error: RecordLength no configurado para FIXED_BINARY."); // [13, 14]
                    return null;
                }

                // Leer EXACTAMENTE los bytes del primer registro en el disco local
                byte[] headerBuffer = new byte[recordLength];
                int bytesRead = in.read(headerBuffer);

                if (bytesRead > 0) {
                    context.getContainer().getLogger().info("Header FIXED_BINARY leído con éxito de " + bytesRead + " bytes."); // [13, 14]
                    return new String(headerBuffer, 0, bytesRead, "UTF-8"); // Ajustar codificación si es ASCII/EBCDIC [15, 16]
                }

            } else {
                // FIXED_TEXT o VARIABLE_TEXT: usamos BufferedReader
                // Usamos un buffer de lectura inicial de tamaño pequeño (ej. 8KB)
                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8192);

                // Lee SOLO la primera línea física y se detiene inmediatamente
                String firstLine = reader.readLine();

                if (firstLine != null) {
                    context.getContainer().getLogger().info("Header de texto leído con éxito. Largo: " + firstLine.length() + " caracteres."); // [13, 14]
                    return firstLine;
                }
            }

        } catch (Exception e) {
            context.getContainer().getLogger().error("Error al extraer el Header del archivo gigante", e); // [13, 14]
        } finally {
            // 4. Asegurar SIEMPRE el cierre de los flujos de lectura en el bloque finally [17]
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputData != null) {
                    inputData.close(); // Crucial para liberar el archivo temporal en el Filer de B2Bi [2, 10]
                }
            } catch (Exception ex) {
                // Ignorar excepciones al cerrar recursos
            }
        }
        return null;
    }
}
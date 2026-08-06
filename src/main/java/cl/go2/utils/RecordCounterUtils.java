package cl.go2.utils;
import com.axway.xib.Context;
import com.axway.xib.ProcessingMessage;

public class RecordCounterUtils {

    public static long calculateFixedRows(Context context, ProcessingMessage message, int largoRegistro) {
        try {
            // 1. Recuperar FileSize desde los atributos del mensaje (tipo long para evitar overflow)
            Object fileSizeObj = message.getAttribute("FileSize");
            long fileSize = 0;
            if (fileSizeObj instanceof Number) {
                fileSize = ((Number) fileSizeObj).longValue();
            } else if (fileSizeObj instanceof String) {
                fileSize = Long.parseLong((String) fileSizeObj);
            }

            // 2. Recuperar el formato de registro (FIXED_BINARY o FIXED_TEXT)
            String recordFormat = (String) message.getAttribute("LocalFileRecordFormat");
            if (recordFormat == null) {
                recordFormat = "FIXED_TEXT"; // Valor por defecto si no está definido
            }

            // 3. Recuperar el largo del registro (RecordLength)
            Object recLenObj = message.getAttribute("LocalFileRecordLength");
            int recordLength = 0;
            if (recLenObj instanceof Number) {
                recordLength = ((Number) recLenObj).intValue();
            } else if (recLenObj instanceof String) {
                recordLength = Integer.parseInt((String) recLenObj);
            }

            //Si no podemos obtener el largo del registro, directamente desde el archivo, usamos el parámetro
            recordLength = largoRegistro;


            if (fileSize <= 0 || recordLength <= 0) {
                context.getContainer().getLogger().warn("No se pudo calcular: FileSize o RecordLength inválidos.");
                return -1;
            }

            long totalRows = 0;

            // 4. Calcular según el formato
            if ("FIXED_TEXT".equalsIgnoreCase(recordFormat)) {
                // Determinar el tamaño del EOL según la plataforma operativa del servidor
                int eolSize = System.lineSeparator().length();
                totalRows = fileSize / (recordLength + eolSize);

                context.getContainer().getLogger().info("Calculado FIXED_TEXT con EOL de " + eolSize + " bytes. Filas: " + totalRows);
            } else {
                // Para FIXED_BINARY o por defecto, división directa sin EOL
                totalRows = fileSize / recordLength;

                context.getContainer().getLogger().info("Calculado FIXED_BINARY sin salto de línea. Filas: " + totalRows);
            }

            return totalRows;

        } catch (Exception e) {
            context.getContainer().getLogger().error("Error al calcular el número de filas", e);
            return -1;
        }
    }
}

package cl.go2.utils;
import com.axway.xib.Context;
import com.axway.xib.ProcessingMessage;
import com.axway.xib.Logger;

/**
 * Clase utilitaria para depuración y monitoreo de metadatos en Axway B2Bi.
 */
public class B2BiMetadataLogger {

    /**
     * Escribe todos los atributos y metadatos disponibles del mensaje
     * directamente en el Trace Viewer de B2Bi de forma segura.
     *
     * @param context El objeto de contexto provisto por el contenedor JMC.
     * @param message El mensaje que se está procesando actualmente.
     */
    public static void logAllAttributes(Context context, ProcessingMessage message) {
        if (context == null || message == null) {
            return;
        }

        // Obtener el Logger nativo asignado al contenedor de la JMC
        Logger logger = context.getContainer().getLogger();

        logger.info("=================================================================");
        logger.info("=== [JMC DEBUG] INICIO DE VOLCADO DE METADATOS DEL MENSAJE ===");
        logger.info("=================================================================");

        // 1. INTENTAR LECTURA DINÁMICA DE ATRIBUTOS
        try {
            // El API del contenedor HME permite listar los nombres de los atributos registrados
            String[] attributeNames = message.getAttributeNames();

            if (attributeNames != null && attributeNames.length > 0) {
                logger.info(">>> Detectados " + attributeNames.length + " atributos dinámicos en el HME:");
                for (String name : attributeNames) {
                    try {
                        Object value = message.getAttribute(name);
                        String valueStr = (value != null) ? value.toString() : "null";
                        logger.info("    [Dinámico] " + name + " = " + valueStr);
                    } catch (Exception ex) {
                        logger.warn("    [Dinámico] " + name + " -> (Error al leer valor: " + ex.getMessage() + ")");
                    }
                }
            } else {
                logger.info(">>> No se listaron atributos dinámicos mediante getAttributeNames().");
            }
        } catch (Exception e) {
            logger.warn(">>> El motor de ejecución no soporta la lectura dinámica de llaves: " + e.getMessage());
        }

        // 2. BARRIDO DE SEGURIDAD (ATRIBUTOS ESTÁNDAR Y CRÍTICOS)
        // Lista exhaustiva de metadatos predefinidos en B2Bi según documentación oficial
        String[] standardAttributes = {
                // Metadatos Básicos del Mensaje
                "CoreId", "ConsumptionFilename", "FileSize", "B2BiConsumptionTimeStamp",
                "ConsumptionUrl", "ConsumptionExchangePointId", "PickupName", "Direction",
                "ContentMimeType", "BusinessProtocol", "BusinessProtocolVersion", "DocumentType",
                "SenderPartyId", "SenderPartyName", "ReceiverPartyId", "ReceiverPartyName",
                "SenderRoutingId", "ReceiverRoutingId",

                // Atributos de Acuerdos y Seguimiento (Tracked Object)
                "AgreementName", "MetadataProfileName", "InboundAgreementName", "OutboundAgreementName",
                "AgreementType", "ServiceName", "MessageFormat", "MessageType", "MessageVersion",
                "DocumentId", "InterchangeId",

                // Atributos de Transcodificación y Formato de Archivo
                "LocalFileCharSet", "VirtualFileCharSet", "LocalFileRecordFormat", "LocalFileRecordLength",

                // Atributos de Transporte Comunes (AS2, PeSIT, etc.)
                "SubjectHeader", "pesit.filename", "pesit.filelabel", "pesit.callerId", "pesit.serverId"
        };

        logger.info("-----------------------------------------------------------------");
        logger.info(">>> Comprobando presencia de metadatos estándar de B2Bi:");
        logger.info("-----------------------------------------------------------------");

        int foundCount = 0;
        for (String attr : standardAttributes) {
            try {
                Object val = message.getAttribute(attr);
                if (val != null) {
                    logger.info("    [Estándar] " + attr + " = " + val.toString());
                    foundCount++;
                }
            } catch (Exception ex) {
                // Ignorar fallos individuales para no interrumpir el flujo completo
            }
        }

        if (foundCount == 0) {
            logger.info("    No se encontraron metadatos estándar poblados en esta etapa.");
        }

        logger.info("=================================================================");
        logger.info("=== [JMC DEBUG] FIN DE VOLCADO DE METADATOS DEL MENSAJE ======");
        logger.info("=================================================================");
    }
}
package cl.go2.b2bi.jmc.foliador;

import cl.go2.b2bi.jmc.foliador.model.FoliadorResultDTO;
import cl.go2.b2bi.jmc.foliador.service.FoliadorService;
import cl.go2.utils.RecordCounterUtils;
import cl.go2.utils.Utils;
import com.axway.xib.MessageScope;
import com.axway.xib.ProcessingMessage;
import com.axway.xib.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import com.axway.xib.Data;

/**
 * Componente JMC Ligero para Axway B2Bi.
 * Se encarga de procesar las peticiones mediante Sub-Threads y propagar el resultado
 * como atributos del mensaje (set/get Attributes).
 */
public class FoliadorJMCComponent {

    private static final Logger logger = LoggerFactory.getLogger(FoliadorJMCComponent.class);
    private final FoliadorService service = new FoliadorService();
    private String configFilePath;          //ruta archivo propiedades conexión BD


    public FoliadorJMCComponent(String configFilePath){
        this.configFilePath = configFilePath;
    }
    /**
     * Método principal invocado por el pipeline de Axway B2Bi.
     *
     * @param messageAttributes Mapa de atributos de metadatos de Axway.
     * @param fileContent Contenido raw del archivo entrante.
     * @return Contenido del archivo o mensaje modificado si se requiere.
     */
    public FoliadorResultDTO executeJMC(String usuarioCasilla,String cleanedFilename , Context context, ProcessingMessage processingMessage, long fileSize) {


        logger.info("FoliadorJMCComponent: Iniciando requerimiento asíncrono para archivo {}", cleanedFilename);


        long cantLineas = RecordCounterUtils.calculateFixedRows(context, processingMessage);
        //Se inyecta el archivo de propiedades
        service.setConfigFilePath(this.configFilePath);

        try {

            // Espera el resultado del sub-thread con timeout de resiliencia
           return service.executeFoliadoLogic(usuarioCasilla,cleanedFilename,context,processingMessage,fileSize,cantLineas);
            
        } catch (Exception e) {
            logger.error("FoliadorJMCComponent: Error durante la foliación en el sub-thread", e);
          
        }
        return null;
    }
}

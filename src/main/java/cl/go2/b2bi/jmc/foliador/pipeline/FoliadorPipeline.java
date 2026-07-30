package cl.go2.b2bi.jmc.foliador.pipeline;

import cl.go2.b2bi.jmc.foliador.FoliadorJMCComponent;
import cl.go2.b2bi.jmc.foliador.model.FoliadorResultDTO;
import cl.go2.utils.RecordCounterUtils;
import cl.go2.utils.Utils;
import com.axway.xib.Component;
import com.axway.xib.Creator;
import com.axway.xib.Context;
import com.axway.xib.ProcessingMessage;
import com.axway.xib.Activity;
import cl.go2.utils.B2BiMetadataLogger;

import com.axway.xib.MessageScope;
import com.axway.xib.Data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class FoliadorPipeline extends Component implements Creator {

    int USUARIO_CASILLA = 0;
    int FILENAME = 1;
    private String configFilePath;          //Especifica el path del archivo de propiedades para acceder a BD.

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }


    @Override
    public void process(Context context, ProcessingMessage processingMessage, Activity[] nextActivities) {
        FoliadorJMCComponent component = new FoliadorJMCComponent(getConfigFilePath());
        try {
            //Dejamos registro del archivo recibido en el trace log
            B2BiMetadataLogger.logAllAttributes(context, processingMessage);
            //Data inputData = processingMessage.getData();
            // Recuperar el nombre del archivo original desde los metadatos
            String originalFilename = (String) processingMessage.getAttribute("ConsumptionFilename");
            long fileSize   = Utils.getFileSize(processingMessage.getAttribute("FileSize"));
            if (originalFilename != null ) {
                context.getContainer().getLogger().info("Procesando archivo gigante: " + originalFilename);
                String[] data_fn = Utils.tokenize_filename(originalFilename);

                //Se copia el archivo de entrada en el buffer de salida.
                ProcessingMessage resultMessage = processingMessage.createMessage(MessageScope.CONTAINER);
                //Inyectamos los metadatos del flujo previo
                resultMessage.setAttribute("ConsumptionFilename", originalFilename);
                resultMessage.setAttribute("FileSize", fileSize); //

                //parseo y generación de folios
                FoliadorResultDTO result = component.executeJMC(data_fn[USUARIO_CASILLA], data_fn[FILENAME], context, processingMessage, fileSize);

                if( null != result){
                    if (nextActivities != null && nextActivities.length > 1) {
                        resultMessage.setActivity(nextActivities[1]); //
                    }
                    //Insumo para el ruteador
                    resultMessage.setAttribute("RDC.TipoDocumento",result.gettDoc());
                    resultMessage.setAttribute("RDC.Foliador.Status","SUCCESS");
                    resultMessage.setAttribute("Foliador", result.toJson());

                }else{
                    //Hubo falla
                    resultMessage.setAttribute("RDC.Foliador.Status","FAIL");
                }


            } else {
                context.getContainer().getLogger().warn("El atributo 'ConsumptionFilename' es nulo. Verifique el protocolo de origen.");
            }

        } catch (Exception e) {
            context.getContainer().getLogger().error("Error al obtener o parsear el nombre del archivo", e);
        }
    }
}

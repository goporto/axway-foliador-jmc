package cl.go2.b2bi.jmc.foliador;

import cl.go2.b2bi.jmc.foliador.pipeline.FoliadorPipeline;
import com.axway.xib.ProcessingMessage;
import com.axway.xib.Activity;
import com.axway.xib.MessageScope;
import com.axway.xib.simulator.Simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clase JUnit 5 de Prueba para la JMC FoliadorPipeline.
 */
public class FoliadorPipelineTest {

    private FoliadorPipeline foliador;
    private byte[] mockPayload;

    @BeforeEach
    protected void setUp() throws Exception {
        // Instanciar el componente bajo prueba
        foliador = new FoliadorPipeline();

        // Propiedad JavaBean configurada para apuntar al archivo de propiedades de prueba
        foliador.setConfigFilePath("C:\\Users\\gopor\\OneDrive\\Documentos\\socius\\POC\\props\\db.properties");


        // Simular el contenido del archivo gigante usando un Header maqueta (1 KB es suficiente)
        //mockPayload = ("HEADER_RDC01_20260730_0001\nCuerpo de prueba...").getBytes("UTF-8");
        mockPayload = Simulator.getResourceContent("RDC01_test02_BUENO 1.txt");

    }

    /**
     * Prueba el flujo de procesamiento de FoliadorPipeline de forma local.
     */
    @Test
    @DisplayName("Prueba de ejecución local para FoliadorPipeline usando Axway Simulator")
    public void testFoliadorPipelineProcess() {
        try {


            // 3. Configurar la propiedad JavaBean (configFilePath)

            // 4. Crear los objetos de mensajería y actividades de B2Bi simulados
            ProcessingMessage inMessage = Simulator.createProcessingMessage(mockPayload);
            Activity activity0 = Simulator.createActivity(1, "actividadFoliadoraInicial");
            Activity activity1 = Simulator.createActivity(2, "actividadRuteadoraDestino");
            Activity[] nextActivities = new Activity[] { activity0, activity1 };

            // Opcional: Si tu JMC requiere atributos de metadatos de entrada, puedes estamparlos
            inMessage.setAttribute("ConsumptionFilename", "web-desabchi_TEST_GIGANTE.txt");
            inMessage.setAttribute("FileSize", "5000000000");
            inMessage.setAttribute("LocalFileRecordFormat", "322");
            // 5. Ejecutar la JMC en el simulador local (100% aislado del servidor)
            Simulator.execute(foliador, inMessage, nextActivities);

            // 6. Recuperar los mensajes resultantes generados de tipo CONTAINER
            ProcessingMessage[] outMessages = inMessage.getMessages(MessageScope.CONTAINER);
            assertNotNull(outMessages);
            assertTrue(outMessages.length > 0);
            ProcessingMessage resultMessage = outMessages[0];
            // Validar que el estatus del foliador sea SUCCESS
            String status = (String) resultMessage.getAttribute("RDC.Foliador.Status");
            assertEquals("SUCCESS", status, "El estatus del foliador debe ser SUCCESS");
            // Validar que el tipo de documento no sea nulo (inyectado desde result.gettDoc())
            String tipoDoc = (String) resultMessage.getAttribute("RDC.TipoDocumento");
            assertNotNull("El atributo RDC.TipoDocumento no debe ser nulo", tipoDoc);

            // Validar la existencia del payload serializado JSON del foliador
            String jsonFoliador = (String) resultMessage.getAttribute("Foliador");
            assertNotNull("El atributo 'Foliador' con el JSON no debe ser nulo", jsonFoliador);
            assertTrue( jsonFoliador.contains("{"),"El JSON del foliador debe contener datos estructurados");

            // 6. VALIDACIÓN DE ENRUTAMIENTO (Actividad Destino)
            // Tu JMC ejecutó: resultMessage.setActivity(nextActivities[5])
            // Validamos que la actividad asignada al mensaje resultante sea efectivamente la segunda ("actividadRuteadoraDestino")
            assertEquals(activity1.getName(), resultMessage.getActivity().getName(), "El mensaje resultante debe dirigirse a la segunda actividad (índice 1)"); // [8, 9]

            System.out.println("[JUnit SUCCESS] Test completado. Atributos inyectados para el ruteador con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Excepción inesperada en la lógica JMC: " + e.getMessage());
        }
    }
}
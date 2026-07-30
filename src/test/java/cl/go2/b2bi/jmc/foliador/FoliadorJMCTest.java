package cl.go2.b2bi.jmc.foliador;

import cl.go2.b2bi.jmc.foliador.config.DatabaseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FoliadorJMCTest {

    private static FoliadorJMCComponent jmcComponent;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        // Inicializamos el componente principal
        jmcComponent = new FoliadorJMCComponent();
        objectMapper = new ObjectMapper();

        // Carga explícita del pool de BD (PostgreSQL según db.properties)
        DatabaseFactory.initialize();
    }

    @AfterAll
    public static void tearDown() {
        // Cierre de conexiones al finalizar las pruebas
        DatabaseFactory.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("Prueba de Foliación Exitosa - Flujo Feliz")
    public void testExecuteJMC_Success() throws Exception {
        // 1. Preparación de Metadatos de Entrada (Simulación de Axway B2Bi)
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put("usuario_casilla", "CASILLA_01");
        messageAttributes.put("cleaned_filename", "FACTURA_TEST_001.txt");

        // 2. Simulación de contenido del archivo con cabecera válida
        // Posiciones: tDocHeader (0-3) = 'FAC', filial (3-7) = '0001', fecha (7-15) = '20260728'
        String fileContent = "FAC000120260728DATOS_DEL_DOCUMENTO_LIGERO_LNEA_1\nLINEA_2_DETALLE_FACTURA";

        // 3. Ejecución del JMC
        String returnedContent = jmcComponent.executeJMC(messageAttributes, fileContent);

        // 4. Verificaciones
        assertNotNull(returnedContent, "El contenido del archivo no debe ser nulo");
        assertEquals(fileContent, returnedContent, "El contenido del archivo debe preservarse intacto");

        // Verificación de Atributos del Mensaje Axway
        assertEquals("0", messageAttributes.get("AXWAY_FOLIADOR_STATUS"), "El estatus de foliación debe ser 0 (Éxito)");
        assertNotNull(messageAttributes.get("AXWAY_CORRELATIVO"), "Debe generar un correlativo único");
        assertNotNull(messageAttributes.get("AXWAY_T_DOC"), "Debe identificar el tipo de documento t_doc");

        // Verificación del JSON unificado de salida
        String jsonResult = messageAttributes.get("AXWAY_FOLIADOR_JSON_RESULT");
        assertNotNull(jsonResult, "Debe propagar el atributo AXWAY_FOLIADOR_JSON_RESULT");

        JsonNode rootNode = objectMapper.readTree(jsonResult);
        assertEquals(0, rootNode.get("status").asInt());
        assertEquals("FACTURA_TEST_001.txt", rootNode.get("archivoEntrada").asText());
        assertNotNull(rootNode.get("correlativo").asText());

        System.out.println("✅ TEST 1 EXITOSO - Correlativo Generado: " + messageAttributes.get("AXWAY_CORRELATIVO"));
        System.out.println("   JSON de Salida: " + jsonResult);
    }

    @Test
    @Order(2)
    @DisplayName("Prueba de Rechazo - Casilla Inexistente (NE)")
    public void testExecuteJMC_CasillaInexistente() throws Exception {
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put("usuario_casilla", "CASILLA_NO_EXISTE");
        messageAttributes.put("cleaned_filename", "ARCHIVO_INVALIDO.txt");

        String fileContent = "FAC000120260728DATOS_DEL_DOCUMENTO";

        // Ejecución
        jmcComponent.executeJMC(messageAttributes, fileContent);

        // Debe marcar error de estatus -1 por regla de negocio
        assertEquals("-1", messageAttributes.get("AXWAY_FOLIADOR_STATUS"));
        assertNotNull(messageAttributes.get("AXWAY_FOLIADOR_ERROR"));
        assertTrue(messageAttributes.get("AXWAY_FOLIADOR_ERROR").contains("Casilla no existe"));

        System.out.println("✅ TEST 2 EXITOSO - Rechazo capturado correctamente: " + messageAttributes.get("AXWAY_FOLIADOR_ERROR"));
    }

    @Test
    @Order(3)
    @DisplayName("Prueba de Rechazo - Archivo Vacío")
    public void testExecuteJMC_ArchivoVacio() {
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put("usuario_casilla", "CASILLA_01");
        messageAttributes.put("cleaned_filename", "ARCHIVO_VACIO.txt");

        String fileContent = ""; // Contenido vacío

        // Ejecución
        jmcComponent.executeJMC(messageAttributes, fileContent);

        // Verificación de captura de error
        assertEquals("-1", messageAttributes.get("AXWAY_FOLIADOR_STATUS"));
        assertTrue(messageAttributes.get("AXWAY_FOLIADOR_ERROR").contains("El archivo se encuentra vacío"));

        System.out.println("✅ TEST 3 EXITOSO - Error de archivo vacío validado correctamente.");
    }
}
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mycompany.lab03.HttpRequest;
import com.mycompany.lab03.HttpServer;
import com.mycompany.lab03.controller.GreetingController;
import com.mycompany.lab03.controller.MathController;

public class HttpServerTest {

    private HttpServer httpServer;

    @BeforeEach
    void setUp() {
        httpServer = new HttpServer();
        HttpServer.services = new HashMap<>();
    }

    @Test
    void testDynamicRouteHandling() throws Exception {
        // Simula la carga de un componente dinámico
        HttpServer.services.put("/greeting", GreetingController.class.getDeclaredMethod("greeting", String.class));

        HttpRequest request = new HttpRequest("/greeting", "name=Tomas");
        String response = httpServer.simulateRequests("/greeting", request);

        // Verifica que la respuesta sea correcta
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("Hola Tomas"));
    }

    @Test
    void test404RouteNotFound() throws Exception {
        HttpRequest request = new HttpRequest("/nonexistent", null);
        String response = httpServer.simulateRequests("/nonexistent", request);

        // Verifica que devuelva un 404
        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
        assertTrue(response.contains("{\"error\":\"Ruta no encontrada\"}"));
    }

    @Test
    void testStaticFileServing() throws Exception {
        File testFile = new File("webroot/test.html");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("<html><body>Test File</body></html>");
        }

        // Simula una solicitud GET para el archivo estático
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("GET /test.html HTTP/1.1\r\n\r\n".getBytes()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        httpServer.handleRequest(mockSocket);

        String response = outputStream.toString();

        // Verifica que el archivo estático se sirva correctamente
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("<html><body>Test File</body></html>"));

        // Limpia el archivo de prueba
        testFile.delete();
    }

    @Test
    void testLoadComponents() throws Exception {
        String[] args = { "com.mycompany.lab03.controller.GreetingController" };
        HttpServer.loadComponents(args);

        // Verifica que las rutas dinámicas se carguen correctamente
        assertTrue(HttpServer.services.containsKey("/greeting"));
    }

        @Test
    void testPi() {
        // Llamar al método pi()
        String result = GreetingController.pi("val");

        // Verificar que devuelve el valor correcto de PI
        assertEquals(Double.toString(Math.PI), result, "El valor de PI no coincide");
    }

    @Test
    void testSumCorrectValues() {
        // Llamar al método sum con valores válidos
        Map<String, Object> result = MathController.sum("5", "7");

        // Verificar que la suma es correcta
        assertEquals(5, result.get("a"));
        assertEquals(7, result.get("b"));
        assertEquals(12, result.get("sum"));
    }

    @Test
    void testSumWithInvalidInput() {
        // Llamar al método sum con valores inválidos
        Map<String, Object> result;
        result = MathController.sum("abc", "10");

        // Verificar que se devuelve un error
        assertTrue(result.containsKey("error"));
        assertEquals("Por favor proporciona números válidos para 'a' y 'b'.", result.get("error"));
    }

}

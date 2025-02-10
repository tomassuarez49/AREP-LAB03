package com.mycompany.lab03;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.mycompany.lab03.controller.GetMapping;
import com.mycompany.lab03.controller.RequestParam;
import com.mycompany.lab03.controller.RestController;

public class HttpServer {

    public static Map<String, Method> services = new HashMap<>();
    private static String staticFilesPath = "webroot";

    public static void main(String[] args) throws Exception {
        int port = 32000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en el puerto: " + port);

        // Cargar componentes dinámicamente
        loadComponents(args);

        boolean running = true;
        while (running) {
            try (Socket clientSocket = serverSocket.accept()) {
                handleRequest(clientSocket);
            }
        }
    }

    public static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String requestLine = in.readLine();
        if (requestLine != null && requestLine.startsWith("GET")) {
            String fileName = requestLine.split(" ")[1];

            if (fileName.equals("/")) {
                fileName = "/index.html"; // Redirige a index.html si no se especifica archivo
            }

            // Separar ruta y parámetros
            String[] pathAndQuery = fileName.split("\\?", 2);
            String path = pathAndQuery[0];
            String query = (pathAndQuery.length > 1) ? pathAndQuery[1] : null;

            System.out.println("Ruta solicitada: " + path);

            // Verifica si la ruta es dinámica
            if (services.containsKey(path)) {
                HttpRequest req = new HttpRequest(path, query);
                try {
                    String response = simulateRequests(path, req);
                    out.println(response);
                } catch (Exception e) {
                    e.printStackTrace();
                    out.println("HTTP/1.1 500 Internal Server Error\r\n\r\n{\"error\":\"Error ejecutando servicio\"}");
                }
                return;
            }

            // Manejo de archivos estáticos
            File file = new File(staticFilesPath + fileName);
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(file);
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType);
                out.println();
                sendFile(file, clientSocket.getOutputStream());
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<h1>Archivo no encontrado</h1>");
            }
        }
    }

    private static String getContentType(File file) {
        if (file.getName().endsWith(".html")) return "text/html";
        if (file.getName().endsWith(".css")) return "text/css";
        if (file.getName().endsWith(".js")) return "application/javascript";
        if (file.getName().endsWith(".jpeg") || file.getName().endsWith(".jpg")) return "image/jpeg";
        if (file.getName().endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private static void sendFile(File file, OutputStream out) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void loadComponents(String[] args) throws Exception {
        for (String arg : args) {
            Class<?> c = Class.forName(arg);
            if (!c.isAnnotationPresent(RestController.class)) {
                System.out.println("🚨 La clase " + arg + " no tiene @RestController y será ignorada.");
                continue;
            }
            System.out.println("✅ Cargando controlador: " + arg);
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(GetMapping.class)) {
                    GetMapping annotation = m.getAnnotation(GetMapping.class);
                    services.put(annotation.value(), m);
                    System.out.println("   📌 Ruta registrada: " + annotation.value());
                }
            }
        }
    }

   public static String simulateRequests(String route, HttpRequest req) throws Exception {
    Method m = services.get(route);
    if (m == null) {
        return "HTTP/1.1 404 Not Found\r\n\r\n{\"error\":\"Ruta no encontrada\"}";
    }

    // Construcción de parámetros usando reflexión
    Object[] args = new Object[m.getParameterCount()];
    int i = 0;
    for (java.lang.reflect.Parameter param : m.getParameters()) {
        if (param.isAnnotationPresent(RequestParam.class)) {
            RequestParam annotation = param.getAnnotation(RequestParam.class);
            String value = req.getValues(annotation.value());
            if (value == null) {
                value = annotation.defaultValue();
            }
            args[i] = value;
        }
        i++;
    }

    // Invocar el método dinámicamente
    Object response = m.invoke(null, args);

    // Convertir la respuesta en JSON si es un Map
    if (response instanceof Map) {
        Map<?, ?> responseMap = (Map<?, ?>) response;
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<?, ?> entry : responseMap.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                json.append(entry.getValue());
            } else {
                json.append("\"").append(entry.getValue()).append("\"");
            }
            json.append(", ");
        }
        // Eliminar la última coma y espacio
        if (json.length() > 1) {
            json.setLength(json.length() - 2);
        }
        json.append("}");
        response = json.toString();
    }

    // Retornar la respuesta como texto plano si no es un Map
    return "HTTP/1.1 200 OK\r\n"
            + "Content-Type: application/json\r\n"
            + "\r\n"
            + response.toString();
}

}

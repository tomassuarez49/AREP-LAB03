package com.mycompany.lab03.controller;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MathController {

    @GetMapping("/sum")
    public static Map<String, Object> sum(
            @RequestParam(value = "a", defaultValue = "0") String a,
            @RequestParam(value = "b", defaultValue = "0") String b
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Convertir los parámetros a enteros
            int numA = Integer.parseInt(a);
            int numB = Integer.parseInt(b);
            int result = numA + numB;

            // Agregar valores al mapa de respuesta
            response.put("a", numA);
            response.put("b", numB);
            response.put("sum", result);
        } catch (NumberFormatException e) {
            // Agregar mensaje de error al mapa de respuesta
            response.put("error", "Por favor proporciona números válidos para 'a' y 'b'.");
        }
        return response; // El framework convierte automáticamente el mapa a JSON
    }
}

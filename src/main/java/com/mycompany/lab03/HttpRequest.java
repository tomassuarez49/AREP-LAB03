
package com.mycompany.lab03;


public class HttpRequest {
    private String path;
    private String query;


    public HttpRequest(String path, String query) {
        this.path = path;
        this.query = query;
    }

    public String getPath() {
        return path;
    }

    public String getValues(String key) {
        if (query == null || !query.contains(key + "=")) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue[0].equals(key)) {
                System.out.println("Query: " + query + ", Key: " + key);

                return keyValue.length > 1 ? keyValue[1] : null;
                
            }
        }
        return null;
    }

   
}

package de.komoot.photon.utils;

import java.util.Arrays;

import static spark.Spark.before;
import static spark.Spark.options;

public class CorsFilter {

    //
    /**
     * Enables CORS on requests. This method is an initialization method and should be called once.
     *
     * As a side effect this sets the content type for the response to "application/json"
     *
     * @param origin permitted origin
     * @param methods permitted methods comma separated
     * @param headers permitted headers comma separated
     */
    public static void enableCORS(final String[] origin, final String methods, final String headers) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        if (origin.length == 1) {
            before((request, response) -> {
                response.header("Access-Control-Allow-Origin", origin[0]);
                response.header("Access-Control-Request-Method", methods);
                response.header("Access-Control-Allow-Headers", headers);
                response.type("application/json; charset=UTF-8");
            });
        } else {
            before((request, response) -> {
                String requestOrigin = request.headers("Origin");
                String matchingOrigin = Arrays.stream(origin).filter(s -> s.equalsIgnoreCase(requestOrigin)).findFirst().orElse(origin[0]);

                response.header("Access-Control-Allow-Origin", matchingOrigin);
                response.header("Access-Control-Request-Method", methods);
                response.header("Access-Control-Allow-Headers", headers);
                response.header("Vary", "Origin");
                response.type("application/json; charset=UTF-8");
            });
        }
    }
}

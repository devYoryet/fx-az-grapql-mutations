package com.userrolemgmt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.userrolemgmt.graphql.GraphQLMutationProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphQLMutationFunction {
    private static final Logger LOGGER = Logger.getLogger(GraphQLMutationFunction.class.getName());
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    private final GraphQLMutationProvider graphQLProvider = new GraphQLMutationProvider();

    @FunctionName("graphQLMutation")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql/mutation") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Solicitud recibida para GraphQL Mutation");

        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporcione una mutación GraphQL válida")
                    .build();
        }

        try {
            Map<String, Object> requestMap = gson.fromJson(requestBody, Map.class);
            String query = (String) requestMap.get("query");

            if (query == null || query.trim().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("La mutación GraphQL no puede estar vacía")
                        .build();
            }

            // Manejar correctamente las variables
            Map<String, Object> variables = new HashMap<>();
            if (requestMap.containsKey("variables") && requestMap.get("variables") != null) {
                variables = (Map<String, Object>) requestMap.get("variables");
            }

            // Ejecutar la mutación GraphQL
            Object result = graphQLProvider.executeMutation(query, variables);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(result))
                    .build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar mutación GraphQL", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar mutación GraphQL: " + e.getMessage())
                    .build();
        }
    }
}
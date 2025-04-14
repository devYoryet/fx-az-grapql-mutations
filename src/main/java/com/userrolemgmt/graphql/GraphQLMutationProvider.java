package com.userrolemgmt.graphql;

import com.userrolemgmt.dao.RoleDAO;
import com.userrolemgmt.dao.UserDAO;
import com.userrolemgmt.model.Role;
import com.userrolemgmt.model.User;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLMutationProvider {

    private final GraphQL graphQL;
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    public GraphQLMutationProvider() {
        // Construir el esquema GraphQL
        GraphQLSchema schema = buildSchema();
        this.graphQL = GraphQL.newGraphQL(schema).build();
    }

    private GraphQLSchema buildSchema() {
        try {
            // Cargar el esquema desde un archivo
            InputStream stream = getClass().getClassLoader().getResourceAsStream("mutation.graphql");
            Reader reader = new InputStreamReader(stream);
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(reader);

            // Configurar los resolvers
            RuntimeWiring runtimeWiring = buildWiring();

            // Generar el esquema
            SchemaGenerator schemaGenerator = new SchemaGenerator();
            return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        } catch (Exception e) {
            throw new RuntimeException("Error al construir el esquema GraphQL para mutaciones", e);
        }
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("_dummy", environment -> "dummy"))
                .type("Mutation", typeWiring -> typeWiring
                // Tus resolvers de mutación existentes...
                )

                .type("Mutation", typeWiring -> typeWiring
                        .dataFetcher("createUser", environment -> {
                            // Obtener argumentos
                            String username = environment.getArgument("username");
                            String email = environment.getArgument("email");
                            String passwordHash = environment.getArgument("passwordHash");
                            String firstName = environment.getArgument("firstName");
                            String lastName = environment.getArgument("lastName");
                            Boolean active = environment.getArgument("active");

                            // Crear usuario
                            User user = new User();
                            user.setUsername(username);
                            user.setEmail(email);
                            user.setPasswordHash(passwordHash);
                            user.setFirstName(firstName);
                            user.setLastName(lastName);
                            user.setActive(active != null ? active : true);

                            return userDAO.createUser(user);
                        })
                        .dataFetcher("createRole", environment -> {
                            // Obtener argumentos
                            String roleName = environment.getArgument("roleName");
                            String description = environment.getArgument("description");

                            // Crear rol
                            Role role = new Role();
                            role.setRoleName(roleName);
                            role.setDescription(description);

                            return roleDAO.createRole(role);
                        })
                        .dataFetcher("updateUser", environment -> {
                            // Obtener argumentos
                            Long userId = Long.parseLong(environment.getArgument("userId"));

                            // Primero obtenemos el usuario existente
                            User existingUser = userDAO.getUserById(userId);
                            if (existingUser == null) {
                                throw new RuntimeException("Usuario no encontrado con ID: " + userId);
                            }

                            // Actualizar solo los campos proporcionados
                            if (environment.containsArgument("username")) {
                                existingUser.setUsername(environment.getArgument("username"));
                            }
                            if (environment.containsArgument("email")) {
                                existingUser.setEmail(environment.getArgument("email"));
                            }
                            if (environment.containsArgument("passwordHash")) {
                                existingUser.setPasswordHash(environment.getArgument("passwordHash"));
                            }
                            if (environment.containsArgument("firstName")) {
                                existingUser.setFirstName(environment.getArgument("firstName"));
                            }
                            if (environment.containsArgument("lastName")) {
                                existingUser.setLastName(environment.getArgument("lastName"));
                            }
                            if (environment.containsArgument("active")) {
                                existingUser.setActive(environment.getArgument("active"));
                            }

                            // Actualizar el usuario en la base de datos
                            boolean updated = userDAO.updateUser(existingUser);
                            if (!updated) {
                                throw new RuntimeException("Error al actualizar usuario con ID: " + userId);
                            }

                            // Retornar el usuario actualizado
                            return userDAO.getUserById(userId);
                        })
                        .dataFetcher("deleteUser", environment -> {
                            // Obtener argumentos
                            Long userId = Long.parseLong(environment.getArgument("userId"));

                            // Eliminar el usuario
                            boolean deleted = userDAO.deleteUser(userId);

                            // Retornar el resultado de la operación
                            return deleted;
                        })
                        .dataFetcher("assignRoleToUser", environment -> {
                            // Obtener argumentos
                            Long userId = Long.parseLong(environment.getArgument("userId"));
                            Long roleId = Long.parseLong(environment.getArgument("roleId"));

                            // Asignar rol a usuario
                            userDAO.assignRoleToUser(userId, roleId);

                            // Retornar usuario actualizado
                            return userDAO.getUserById(userId);

                        }))
                .build();
    }

    public Object executeMutation(String mutation, Map<String, Object> variables) {
        // Evitar el error de variables null
        if (variables == null) {
            variables = new HashMap<>();
        }
        return graphQL.execute(mutation, null, null, variables).toSpecification();
    }

}
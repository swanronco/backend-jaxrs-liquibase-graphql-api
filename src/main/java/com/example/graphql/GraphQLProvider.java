package com.example.graphql;

import com.example.graphql.datafetcher.UserMutationDataFetcher;
import com.example.graphql.datafetcher.UserQueryDataFetcher;
import com.example.graphql.resolver.UserMutationResolver;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Objects;

public class GraphQLProvider {

    private GraphQL graphQL;
    private final UserQueryDataFetcher userQueryDataFetcher;
    private final UserMutationDataFetcher userMutationDataFetcher;
    private final UserMutationResolver userMutationResolver = new UserMutationResolver();

    public GraphQLProvider(UserQueryDataFetcher userQueryDataFetcher,
                           UserMutationDataFetcher userMutationDataFetcher) {
        this.userQueryDataFetcher = userQueryDataFetcher;
        this.userMutationDataFetcher = userMutationDataFetcher;
        init();
    }

    private void init() {
        GraphQLSchema graphQLSchema = buildSchema();
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema() {
        TypeDefinitionRegistry typeRegistry = loadSchema();
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private TypeDefinitionRegistry loadSchema() {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();

        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("graphql");
        if (url == null) {
            throw new RuntimeException("Schema directory not found: graphql");
        }
        File schemaDir = new File(url.getFile());
        File[] schemaFiles = schemaDir.listFiles((dir, name) -> name.endsWith(".graphqls"));
        if (schemaFiles == null || schemaFiles.length == 0) {
            throw new RuntimeException("No .graphqls files found in: graphql");
        }

        for (File file : schemaFiles) {
            try (Reader reader = new InputStreamReader(Objects.requireNonNull(classLoader.getResourceAsStream("graphql/" + file.getName())))) {
                typeRegistry.merge(schemaParser.parse(reader));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse schema file: " + file.getName(), e);
            }
        }
        return typeRegistry;
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(DateTimeScalar.DATE_TIME) // Register the custom scalar here
                .type("Query", builder -> builder
                        .dataFetcher("user", userQueryDataFetcher)
                        .dataFetcher("users", userQueryDataFetcher))
                .type("Mutation", builder -> builder
                        .dataFetcher("createUser", userMutationDataFetcher)
                        .dataFetcher("updateUser", userMutationDataFetcher)
                        .dataFetcher("deleteUser", userMutationDataFetcher)
                        .dataFetcher("login", environment -> {
                            String identifier = environment.getArgument("identifier");
                            String password = environment.getArgument("password");
                            return userMutationResolver.login(identifier, password);
                        })
                        .dataFetcher("logout", environment -> {
                            String token = environment.getArgument("token");
                            return userMutationResolver.logout(token);
                        })
                        .dataFetcher("logout", env -> userMutationResolver.logout(env.getArgument("token"))))
                .build();
    }

    public GraphQL buildGraphQL() {
        if (graphQL == null) {
            init();
        }
        return graphQL;
    }
}

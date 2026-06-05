package com.example.graphql.datafetcher;

import com.example.dto.CreateUserResponse;
import com.example.service.UserService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;

public class UserMutationDataFetcher implements DataFetcher<CreateUserResponse> {

    private final UserService userService;

    public UserMutationDataFetcher(UserService userService) {
        this.userService = userService;
    }

    @Override
    public CreateUserResponse get(DataFetchingEnvironment environment) {
        Map<String, Object> input = environment.getArgument("input");
        String firstName = (String) input.get("firstName");
        String lastName = (String) input.get("lastName");
        String email = (String) input.get("email");
        String username = (String) input.get("username");
        String passwordHash = BCrypt.hashpw((String) input.get("password"), BCrypt.gensalt());
        try {
            return userService.createUser(firstName, lastName, email, username, passwordHash);
        } catch (Exception e) {
            String fullMsg = fullMessage(e).toLowerCase();
            if (fullMsg.contains("email")) throw new RuntimeException("Cet email est déjà utilisé.");
            if (fullMsg.contains("username")) throw new RuntimeException("Ce nom d'utilisateur est déjà pris.");
            throw new RuntimeException("Impossible de créer le compte. Veuillez réessayer.");
        }
    }

    private String fullMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            if (t.getMessage() != null) sb.append(t.getMessage()).append(' ');
            t = t.getCause();
        }
        return sb.toString();
    }
}

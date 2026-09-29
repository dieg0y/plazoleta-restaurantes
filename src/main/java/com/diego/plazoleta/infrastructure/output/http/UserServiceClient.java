package com.diego.plazoleta.infrastructure.output.http;

import com.diego.plazoleta.domain.port.UserValidationPort;
import com.diego.plazoleta.domain.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient implements UserValidationPort {
    private final RestClient client;
    private final String roleUrlTemplate;

    public UserServiceClient(RestClient.Builder builder,
                             @Value("${users.role-url-template:http://localhost:8081/internal/usuarios/{usuarioId}/roles/{rol}}") String roleUrlTemplate) {
        this.client = builder.build();
        this.roleUrlTemplate = roleUrlTemplate;
    }

    @Override
    public boolean isRestaurantOwner(String userId, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UserServiceUnavailableException("Authorization is required for user role validation",
                    new IllegalArgumentException("Missing Authorization header"));
        }
        try {
            Boolean owner = client.get().uri(roleUrlTemplate, userId, "ROLE_PROPIETARIO")
                    .header("Authorization", authorization)
                    .retrieve().body(Boolean.class);
            if (owner == null) {
                throw new UserServiceUnavailableException("User service returned an empty role response",
                        new IllegalStateException("Expected a JSON boolean"));
            }
            return owner;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new UserServiceUnavailableException("User service rejected the role lookup", ex);
        } catch (RestClientException ex) {
            throw new UserServiceUnavailableException("Unable to validate the owner's role", ex);
        }
    }
}

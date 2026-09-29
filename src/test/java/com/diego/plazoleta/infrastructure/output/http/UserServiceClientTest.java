package com.diego.plazoleta.infrastructure.output.http;

import com.diego.plazoleta.domain.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserServiceClientTest {
    @Test
    void forwardsAdminAuthorizationAndReadsBooleanOwnerRoleResponse() {
        Fixture plain = fixture();
        plain.server.expect(requestTo("http://users.test/internal/usuarios/42/roles/ROLE_PROPIETARIO"))
                .andExpect(header("Authorization", "Bearer admin-token"))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));
        assertTrue(plain.client.isRestaurantOwner("42", "Bearer admin-token"));
        plain.server.verify();

    }

    @Test
    void returnsFalseForUnknownUsersOrDifferentRoles() {
        Fixture notFound = fixture();
        notFound.server.expect(requestTo("http://users.test/internal/usuarios/404/roles/ROLE_PROPIETARIO"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertFalse(notFound.client.isRestaurantOwner("404", "Bearer admin-token"));
        notFound.server.verify();

        Fixture otherRole = fixture();
        otherRole.server.expect(requestTo("http://users.test/internal/usuarios/43/roles/ROLE_PROPIETARIO"))
                .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));
        assertFalse(otherRole.client.isRestaurantOwner("43", "Bearer admin-token"));
        otherRole.server.verify();
    }

    @Test
    void failsClosedWhenServiceIsUnavailableOrReturnsMalformedData() {
        Fixture unavailable = fixture();
        unavailable.server.expect(requestTo("http://users.test/internal/usuarios/42/roles/ROLE_PROPIETARIO"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(UserServiceUnavailableException.class, () -> unavailable.client.isRestaurantOwner("42", "Bearer admin-token"));
        unavailable.server.verify();

        Fixture malformed = fixture();
        malformed.server.expect(requestTo("http://users.test/internal/usuarios/42/roles/ROLE_PROPIETARIO"))
                .andRespond(withSuccess("unknown", MediaType.TEXT_PLAIN));
        assertThrows(UserServiceUnavailableException.class, () -> malformed.client.isRestaurantOwner("42", "Bearer admin-token"));
        malformed.server.verify();
    }

    @Test
    void rejectsEmptyRoleResponseInsteadOfTreatingItAsNonOwner() {
        Fixture empty = fixture();
        empty.server.expect(requestTo("http://users.test/internal/usuarios/42/roles/ROLE_PROPIETARIO"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(UserServiceUnavailableException.class, () -> empty.client.isRestaurantOwner("42", "admin-token"));
        empty.server.verify();
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new UserServiceClient(builder,
                "http://users.test/internal/usuarios/{usuarioId}/roles/{rol}"), server);
    }

    private record Fixture(UserServiceClient client, MockRestServiceServer server) {
    }
}

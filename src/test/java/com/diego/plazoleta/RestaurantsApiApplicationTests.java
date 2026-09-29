package com.diego.plazoleta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.diego.plazoleta.domain.port.UserValidationPort;
import com.diego.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import com.diego.plazoleta.infrastructure.output.jpa.repository.DishJpaRepository;
import com.diego.plazoleta.infrastructure.output.jpa.repository.RestaurantJpaRepository;
import com.diego.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = "security.jwt.secret=test-signing-secret-that-is-at-least-32-characters")
@AutoConfigureMockMvc
class RestaurantsApiApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired RestaurantJpaRepository restaurants;
    @Autowired DishJpaRepository dishes;
    @MockBean UserValidationPort userValidation;

    @BeforeEach
    void cleanDatabase() {
        dishes.deleteAll();
        restaurants.deleteAll();
        when(userValidation.isRestaurantOwner(eq("1"), anyString())).thenReturn(true);
    }

    @Test
    void endpointsRequireBearerAuthentication() throws Exception {
        mvc.perform(get("/api/v1/restaurants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsJwtSignedWithSharedHs256Secret() throws Exception {
        String token = createToken("1", "ROLE_CLIENTE");

        mvc.perform(get("/api/v1/restaurants").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void restaurantCatalogIsCustomerOnly() throws Exception {
        mvc.perform(get("/api/v1/restaurants")
                        .with(jwt().jwt(jwt -> jwt.subject("2")).authorities(() -> "ROLE_PROPIETARIO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/restaurants")
                        .with(jwt().jwt(jwt -> jwt.subject("2")).authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurantOwnerIdMustBeNumericBeforeUserRoleLookup() throws Exception {
        mvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + createToken("2", "ROLE_ADMIN"))
                        .contentType("application/json").content("""
                                {"name":"Cafe 24","nit":"12345","address":"Main street","phone":"+573001112233",
                                 "logoUrl":"https://example.test/logo.png","ownerId":"owner-1"}
                                """))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(userValidation);
    }

    @Test
    void rejectsJwtWithWrongIssuerOrNonNumericSubject() throws Exception {
        mvc.perform(get("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + createToken("1", "ROLE_CLIENTE", "untrusted-issuer")))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + createToken("client-1", "ROLE_CLIENTE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRestaurantIsAdminOnly() throws Exception {
        mvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + createToken("1", "ROLE_PROPIETARIO"))
                        .contentType("application/json").content("""
                                {"name":"Cafe 24","nit":"12345","address":"Main street","phone":"+573001112233",
                                 "logoUrl":"https://example.test/logo.png","ownerId":"1"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRequestValidationReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/restaurants").with(jwt().jwt(jwt -> jwt.subject("admin"))
                        .authorities(() -> "ROLE_ADMIN"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void listRequiresCustomerOrPermittedRoleAndReturnsOnlyPublicFields() throws Exception {
        restaurants.save(restaurant("Zulu", "900", "owner-1"));
        restaurants.save(restaurant("Alpha", "100", "owner-1"));
        mvc.perform(get("/api/v1/restaurants?page=0&size=1").with(jwt().jwt(jwt -> jwt.subject("5"))
                        .authorities(() -> "ROLE_CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alpha"))
                .andExpect(jsonPath("$.content[0].logoUrl").value("https://example.test/Alpha.png"))
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void adminCanCreateRestaurantAndOwnerCanCreateDish() throws Exception {
        var restaurantResult = mvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + createToken("2", "ROLE_ADMIN"))
                        .contentType("application/json").content("""
                                {"name":"Cafe 24","nit":"12345","address":"Main street","phone":"+573001112233",
                                 "logoUrl":"https://example.test/logo.png","ownerId":"1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId").value("1"))
                .andReturn();
        int restaurantId = com.jayway.jsonpath.JsonPath.read(restaurantResult.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/v1/restaurants/" + restaurantId + "/dishes").with(jwt().jwt(jwt -> jwt.subject("1"))
                        .authorities(() -> "ROLE_PROPIETARIO"))
                        .contentType("application/json").content("""
                                {"name":"Bandeja","price":20000,"description":"Lunch","imageUrl":"https://example.test/dish.png",
                                 "category":"Lunch"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                                .andExpect(jsonPath("$.restaurantId").value(restaurantId));
    }

    @Test
    void internalOwnershipCheckIsProtectedAndConfirmsAuthenticatedProprietor() throws Exception {
        RestaurantEntity entity = restaurants.save(restaurant("Cafe", "345", "1"));
        String path = "/internal/restaurantes/" + entity.getId() + "/propietarios/1";

        mvc.perform(get(path).with(jwt().jwt(jwt -> jwt.subject("1"))
                                        .authorities(() -> "ROLE_PROPIETARIO")))
                                .andExpect(status().isOk())
                                .andExpect(content().string("true"));

        mvc.perform(get("/internal/restaurantes/" + entity.getId() + "/propietarios/2")
                                        .with(jwt().jwt(jwt -> jwt.subject("1")).authorities(() -> "ROLE_PROPIETARIO")))
                                .andExpect(status().isOk())
                                .andExpect(content().string("false"));

        mvc.perform(get(path).with(jwt().jwt(jwt -> jwt.subject("3"))
                                        .authorities(() -> "ROLE_ADMIN")))
                                .andExpect(status().isForbidden());

        mvc.perform(get(path))
                                .andExpect(status().isUnauthorized());
    }

    @Test
    void proprietorCannotManageAnotherOwnersDishes() throws Exception {
        RestaurantEntity restaurant = restaurants.save(restaurant("Cafe", "345", "1"));
        DishEntity dish = new DishEntity();
        dish.setRestaurant(restaurant);
        dish.setName("Soup");
        dish.setPrice(1200);
        dish.setDescription("Hot");
        dish.setImageUrl("https://example.test/soup.png");
        dish.setCategory("Lunch");
        dish.setActive(true);
        dish = dishes.save(dish);

        mvc.perform(post("/api/v1/restaurants/" + restaurant.getId() + "/dishes")
                                        .with(jwt().jwt(jwt -> jwt.subject("2")).authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("""
                                                {"name":"Dish","price":1000,"description":"Desc","imageUrl":"https://example.test/dish.png",
                                                 "category":"Lunch"}
                                                """))
                                .andExpect(status().isForbidden());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/dishes/" + dish.getId())
                                        .with(jwt().jwt(jwt -> jwt.subject("2")).authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("""
                                                {"price":1500,"description":"Changed"}
                                                """))
                                .andExpect(status().isForbidden());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                                "/api/v1/dishes/" + dish.getId() + "/status")
                                        .with(jwt().jwt(jwt -> jwt.subject("2")).authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("""
                                                {"active":false}
                                                """))
                                .andExpect(status().isForbidden());
    }

    private String createToken(String subject, String role) {
        return createToken(subject, role, "plazoleta-usuarios");
    }

    private String createToken(String subject, String role, String issuer) {
        byte[] secret = "test-signing-secret-that-is-at-least-32-characters".getBytes(StandardCharsets.UTF_8);
        Instant now = Instant.now();
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        return encoder.encode(JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                JwtClaimsSet.builder().issuer(issuer).subject(subject).claim("role", role)
                                        .issuedAt(now).expiresAt(now.plusSeconds(60)).build())).getTokenValue();
    }

    @Test
    void dishPriceAndStatusRequestsAreValidated() throws Exception {
        mvc.perform(post("/api/v1/restaurants/1/dishes").with(jwt().jwt(jwt -> jwt.subject("1"))
                                        .authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("""
                                                {"name":"Dish","price":0,"description":"Description","imageUrl":"https://example.test/dish.png",
                                                 "category":"Lunch"}
                                                """))
                                .andExpect(status().isBadRequest());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/dishes/1")
                                        .with(jwt().jwt(jwt -> jwt.subject("1")).authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("""
                                                {"price":-1,"description":"Description"}
                                                """))
                                .andExpect(status().isBadRequest());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/dishes/1/status")
                                        .with(jwt().jwt(jwt -> jwt.subject("1")).authorities(() -> "ROLE_PROPIETARIO"))
                                        .contentType("application/json").content("{}"))
                                .andExpect(status().isBadRequest());
    }

    private RestaurantEntity restaurant(String name, String nit, String owner) {
        RestaurantEntity entity = new RestaurantEntity();
        entity.setName(name);
        entity.setNit(nit);
        entity.setAddress("Main street");
        entity.setPhone("12345");
        entity.setLogoUrl("https://example.test/" + name + ".png");
        entity.setOwnerId(owner);
        return entity;
    }
}

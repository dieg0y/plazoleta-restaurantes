package com.diego.plazoleta.application.usecase;

import com.diego.plazoleta.domain.exception.ForbiddenOperationException;
import com.diego.plazoleta.domain.exception.InvalidInputException;
import com.diego.plazoleta.domain.exception.NotFoundException;
import com.diego.plazoleta.domain.model.Dish;
import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.domain.model.RestaurantPage;
import com.diego.plazoleta.domain.port.DishRepositoryPort;
import com.diego.plazoleta.domain.port.RestaurantRepositoryPort;
import com.diego.plazoleta.domain.port.UserValidationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock RestaurantRepositoryPort restaurants;
    @Mock DishRepositoryPort dishes;
    @Mock UserValidationPort userValidation;
    RestaurantService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantService(restaurants, dishes, userValidation);
    }

    @Test
    void createsRestaurantWhenOwnerRoleIsVerified() {
        Restaurant request = restaurant("La 12", "12345", "+573001112233", "11");
        when(userValidation.isRestaurantOwner("11", "Bearer admin-token")).thenReturn(true);
        when(restaurants.save(request)).thenReturn(new Restaurant(4L, request.name(), request.nit(),
                request.address(), request.phone(), request.logoUrl(), request.ownerId()));

        Restaurant created = service.createRestaurant(request, "Bearer admin-token");

        assertEquals(4L, created.id());
        verify(userValidation).isRestaurantOwner("11", "Bearer admin-token");
        verify(restaurants).save(request);
    }

    @Test
    void rejectsRestaurantNameThatIsBlankOrOnlyDigits() {
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("  ", "123", "12345", "3"), "Bearer admin-token"));
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("12345", "123", "12345", "3"), "Bearer admin-token"));
        verifyNoInteractions(restaurants, userValidation);
    }

    @Test
    void allowsRestaurantNamesContainingDigits() {
        Restaurant request = restaurant("Restaurante 24", "123", "12345", "3");
        when(userValidation.isRestaurantOwner("3", "Bearer admin-token")).thenReturn(true);
        when(restaurants.save(request)).thenReturn(request);
        assertEquals(request, service.createRestaurant(request, "Bearer admin-token"));
    }

    @Test
    void rejectsNonNumericNitAndInvalidPhone() {
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("Cafe", "12-A", "1234", "3"), "Bearer admin-token"));
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("Cafe", "12", "12A", "3"), "Bearer admin-token"));
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("Cafe", "12", "+1234567890123", "3"), "Bearer admin-token"));
        verifyNoInteractions(userValidation, restaurants);
    }

    @Test
    void acceptsPhoneWithThirteenDigitsWithoutPlus() {
        Restaurant request = restaurant("Cafe 13", "12", "1234567890123", "3");
        when(userValidation.isRestaurantOwner("3", "admin-token")).thenReturn(true);
        when(restaurants.save(request)).thenReturn(request);

        assertEquals(request, service.createRestaurant(request, "admin-token"));
        verify(userValidation).isRestaurantOwner("3", "admin-token");
    }

    @Test
    void rejectsOwnerIdOutsidePositiveLongRange() {
        Restaurant request = restaurant("Cafe", "12", "1234", "9999999999999999999");

        assertThrows(InvalidInputException.class, () -> service.createRestaurant(request, "admin-token"));
        verifyNoInteractions(userValidation, restaurants);
    }

    @Test
    void rejectsUnknownOrNonOwnerUser() {
        Restaurant request = restaurant("Cafe", "12", "1234", "3");
        when(userValidation.isRestaurantOwner("3", "Bearer admin-token")).thenReturn(false);
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(request, "Bearer admin-token"));
        verify(restaurants, never()).save(any());
    }

    @Test
    void validatesRequiredOwnerId() {
        assertThrows(InvalidInputException.class, () -> service.createRestaurant(restaurant("Cafe", "12", "1234", " "), "Bearer admin-token"));
        verifyNoInteractions(userValidation, restaurants);
    }

    @Test
    void listsRestaurantsWithValidatedPagination() {
        RestaurantPage page = new RestaurantPage(List.of(restaurant("A", "1", "1", "3")), 2, 5, 11, 3);
        when(restaurants.findAllAlphabetically(2, 5)).thenReturn(page);
        assertEquals(page, service.listRestaurants(2, 5));
        assertThrows(InvalidInputException.class, () -> service.listRestaurants(-1, 5));
        assertThrows(InvalidInputException.class, () -> service.listRestaurants(0, 0));
        assertThrows(InvalidInputException.class, () -> service.listRestaurants(0, 101));
        verify(restaurants).findAllAlphabetically(2, 5);
    }

    @Test
    void createsDishAsOwnerAndDefaultsActive() {
        when(restaurants.findById(7L)).thenReturn(Optional.of(restaurantWithId(7L, "owner")));
        when(dishes.save(any())).thenAnswer(call -> call.getArgument(0));
        Dish input = new Dish(null, null, "Bandeja", 24000, "Plato del día", "https://image.test/x",
                "Almuerzo", true);

        Dish result = service.createDish(7L, "owner", input);

        assertEquals(7L, result.restaurantId());
        assertTrue(result.active());
        assertEquals("Bandeja", result.name());
    }

    @Test
    void rejectsDishCreationByDifferentOwnerOrMissingRestaurant() {
        when(restaurants.findById(7L)).thenReturn(Optional.of(restaurantWithId(7L, "owner")));
        Dish input = new Dish(null, null, "Bandeja", 1, "Desc", "https://image.test/x", "Food", true);
        assertThrows(ForbiddenOperationException.class, () -> service.createDish(7L, "intruder", input));
        when(restaurants.findById(8L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.createDish(8L, "owner", input));
        verifyNoInteractions(dishes);
    }

    @Test
    void updatesOnlyDishPriceAndDescription() {
        Dish old = new Dish(5L, 7L, "Bandeja", 20000, "Old", "https://image.test/x", "Almuerzo", false);
        when(dishes.findById(5L)).thenReturn(Optional.of(old));
        when(restaurants.findById(7L)).thenReturn(Optional.of(restaurantWithId(7L, "owner")));
        when(dishes.save(any())).thenAnswer(call -> call.getArgument(0));

        Dish updated = service.updateDish(5L, "owner", 25000, "New description");

        assertEquals(25000, updated.price());
        assertEquals("New description", updated.description());
        assertEquals("Bandeja", updated.name());
        assertEquals("https://image.test/x", updated.imageUrl());
        assertEquals("Almuerzo", updated.category());
        assertFalse(updated.active());
    }

    @Test
    void dishChangesRequireOwnershipAndExistingDish() {
        Dish old = new Dish(5L, 7L, "Bandeja", 20000, "Old", "url", "Almuerzo", true);
        when(dishes.findById(5L)).thenReturn(Optional.of(old));
        when(restaurants.findById(7L)).thenReturn(Optional.of(restaurantWithId(7L, "owner")));
        assertThrows(ForbiddenOperationException.class, () -> service.updateDish(5L, "other", 1, "x"));
        assertThrows(ForbiddenOperationException.class, () -> service.setDishActive(5L, "other", false));
        when(dishes.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateDish(99L, "owner", 1, "x"));
    }

    @Test
    void togglesDishStatusWithoutChangingOtherFields() {
        Dish old = new Dish(5L, 7L, "Bandeja", 20000, "Desc", "url", "Almuerzo", true);
        when(dishes.findById(5L)).thenReturn(Optional.of(old));
        when(restaurants.findById(7L)).thenReturn(Optional.of(restaurantWithId(7L, "owner")));
        when(dishes.save(any())).thenAnswer(call -> call.getArgument(0));
        Dish deactivated = service.setDishActive(5L, "owner", false);
        assertFalse(deactivated.active());
        assertEquals(old.name(), deactivated.name());
        assertEquals(old.price(), deactivated.price());
        assertEquals(old.description(), deactivated.description());
        assertEquals(old.imageUrl(), deactivated.imageUrl());
        assertEquals(old.category(), deactivated.category());
    }

    private Restaurant restaurant(String name, String nit, String phone, String owner) {
        return new Restaurant(null, name, nit, "Main street 1", phone, "https://image.test/logo", owner);
    }

    private Restaurant restaurantWithId(Long id, String owner) {
        return new Restaurant(id, "Cafe", "12", "Main street 1", "1234", "https://image.test/logo", owner);
    }
}

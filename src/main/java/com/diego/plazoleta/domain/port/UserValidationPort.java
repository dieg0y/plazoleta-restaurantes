package com.diego.plazoleta.domain.port;

public interface UserValidationPort {
    boolean isRestaurantOwner(String userId, String authorization);
}

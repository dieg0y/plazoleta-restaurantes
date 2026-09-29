CREATE TABLE restaurants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    nit VARCHAR(30) NOT NULL,
    address VARCHAR(250) NOT NULL,
    phone VARCHAR(13) NOT NULL,
    logo_url VARCHAR(500) NOT NULL,
    owner_id VARCHAR(100) NOT NULL,
    CONSTRAINT pk_restaurants PRIMARY KEY (id),
    CONSTRAINT uk_restaurant_nit UNIQUE (nit)
);

CREATE TABLE dishes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    price INTEGER NOT NULL,
    description VARCHAR(1000) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    category VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT pk_dishes PRIMARY KEY (id),
    CONSTRAINT fk_dishes_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);

CREATE INDEX idx_dishes_restaurant_id ON dishes (restaurant_id);

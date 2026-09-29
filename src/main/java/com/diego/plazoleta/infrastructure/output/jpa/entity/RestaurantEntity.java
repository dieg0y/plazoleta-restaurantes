package com.diego.plazoleta.infrastructure.output.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurants", uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_nit", columnNames = "nit"))
public class RestaurantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 30)
    private String nit;
    @Column(nullable = false, length = 250)
    private String address;
    @Column(nullable = false, length = 13)
    private String phone;
    @Column(nullable = false, length = 500)
    private String logoUrl;
    @Column(nullable = false, length = 100)
    private String ownerId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}

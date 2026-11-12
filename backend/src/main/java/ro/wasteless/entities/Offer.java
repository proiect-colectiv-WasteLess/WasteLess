package ro.wasteless.entities;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
public class Offer {
    public enum Category { Bakery, Cafe, Restaurant, Grocery, Other }
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(optional=false) @JoinColumn(name="business_id") public UserAccount business;
    @Column(nullable=false) public String productName;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Category category;
    @Column(nullable=false, length=2000) public String description;
    @Column(nullable=false, precision=12, scale=2) public BigDecimal originalPrice;
    @Column(nullable=false, precision=12, scale=2) public BigDecimal wasteLessPrice;
    @Column(nullable=false, length=3) public String currency;
    @Column(nullable=false) public int quantity;
    @Column(nullable=false) public String pickupLocation;
    @Column(nullable=false) public Instant pickupStart;
    @Column(nullable=false) public Instant pickupEnd;
    @Column(nullable=false) public Instant createdAt = Instant.now();
    @Column(length=2048) public String imageUrl;
    public Offer() {}
}
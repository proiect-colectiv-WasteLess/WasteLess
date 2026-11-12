package ro.wasteless.entities;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
public class Reservation {
    public enum Status { RESERVED }
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(optional=false) @JoinColumn(name="client_id") public UserAccount client;
    @ManyToOne(optional=false) @JoinColumn(name="offer_id") public Offer offer;
    @Column(nullable=false) public int quantity;
    @Column(nullable=false) public Instant createdAt = Instant.now();
    @Column(nullable=false, precision=12, scale=2) public BigDecimal unitPrice;
    @Column(nullable=false, precision=22, scale=2) public BigDecimal total;
    @Column(nullable=false, length=3) public String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status = Status.RESERVED;
    public Reservation() {}
}
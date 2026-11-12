package ro.wasteless.entities;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="user_account")
public class UserAccount {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable=false, unique=true) public String email;
    @Column(nullable=false) public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role;
    public String businessName;
    public String pickupLocation;
    public UserAccount() {}
}
package ro.wasteless.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends ActionToken {}

package ro.wasteless.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends ActionToken {}

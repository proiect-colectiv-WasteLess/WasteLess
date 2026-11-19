package ro.wasteless.auth.api;

import java.time.Instant;
import java.util.*;

public record UserView(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    String avatarUrl,
    String preferredLocale,
    String status,
    boolean emailVerified,
    List<String> roles,
    boolean locationSharingEnabled,
    boolean marketingOptIn,
    Instant memberSince) {}

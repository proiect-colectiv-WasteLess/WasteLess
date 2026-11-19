package ro.wasteless.auth.api;

public record SessionTokens(
    String accessToken, String refreshToken, long expiresIn, String tokenType, UserView user) {
  @Override
  public String toString() {
    return "SessionTokens[redacted]";
  }
}

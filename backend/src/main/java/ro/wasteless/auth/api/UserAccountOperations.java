package ro.wasteless.auth.api;

import java.util.Map;

/** Public account operations used by the user HTTP adapter. */
public interface UserAccountOperations {
  UserView me();

  UserView patch(Map<String, Object> values);

  void changePassword(String oldPassword, String newPassword);

  void putLocation(AccountRequests.Location request);

  void deleteLocation();

  void deleteAccount(String password);
}

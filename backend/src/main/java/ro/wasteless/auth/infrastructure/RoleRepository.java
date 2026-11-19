package ro.wasteless.auth.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepository {
  private final JdbcTemplate jdbc;

  public RoleRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> codes(UUID user) {
    return jdbc.queryForList(
        "select r.code from roles r join user_roles ur on ur.role_id=r.id where ur.user_id=? order by r.code",
        String.class,
        user);
  }

  public void add(UUID user, String role, UUID actor, Instant now) {
    int count =
        jdbc.update(
            "insert into user_roles(user_id,role_id,assigned_at,assigned_by) select ?,id,?,? from roles where code=? on conflict do nothing",
            user,
            Timestamp.from(now),
            actor,
            role);
    if (count == 0 && !codes(user).contains(role))
      throw new IllegalStateException("Required role seed is missing: " + role);
  }

  public void remove(UUID user, String role) {
    jdbc.update(
        "delete from user_roles where user_id=? and role_id=(select id from roles where code=?)",
        user,
        role);
  }

  public void lockAdministration() {
    jdbc.execute("select pg_advisory_xact_lock(730015)");
  }

  public long activeAdmins() {
    return jdbc.queryForObject(
        "select count(*) from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id where r.code='ROLE_ADMIN' and u.status='ACTIVE' and u.deleted_at is null",
        Long.class);
  }
}

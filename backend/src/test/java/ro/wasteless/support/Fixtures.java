package ro.wasteless.support;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Each factory creates unique IDs; use in rollback transactions for test isolation. */
public final class Fixtures {
  private final JdbcTemplate jdbc;

  public Fixtures(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public UUID user() {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into users(id,email,password_hash) values (?,?,?)",
        id,
        id + "@example.test",
        "test-only");
    return id;
  }

  public UUID business(UUID owner) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into businesses(id,owner_user_id,name,slug) values (?,?,?,?)",
        id,
        owner,
        "Fixture Bakery",
        id.toString());
    return id;
  }

  public UUID location(UUID business) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into business_locations(id,business_id,address_line1,city,country_code,latitude,longitude,is_primary) values (?,?,'Test street','Bucharest','RO',44.4268,26.1025,true)",
        id,
        business);
    return id;
  }

  public UUID category() {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into offer_categories(id,slug,display_name) values (?,?,?)",
        id,
        id.toString(),
        "Bakery");
    return id;
  }

  public UUID offer(UUID business, UUID location, UUID category) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into offers(id,business_id,location_id,category_id,title,original_price,price,quantity_total,quantity_available,pickup_start_at,pickup_end_at) values (?,?,?,?,'Bag',12,4,10,10,now()+interval '1 hour',now()+interval '3 hours')",
        id,
        business,
        location,
        category);
    return id;
  }

  public UUID order(UUID user, UUID business, UUID location, UUID offer) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into orders(id,reference,customer_user_id,business_id,business_location_id,offer_id,status,quantity,unit_price,original_unit_price,total_price,item_name,business_name,pickup_start_at,pickup_end_at,pickup_code) values (?,?,?,?,?,?,'ACTIVE',1,4,12,4,'Bag','Bakery',now()+interval '1 hour',now()+interval '3 hours','123456')",
        id,
        "T" + id.toString().replace("-", "").substring(0, 19),
        user,
        business,
        location,
        offer);
    return id;
  }

  public UUID payment(UUID order, UUID user) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into payments(id,order_id,user_id,method,status,amount,currency,provider) values (?,?,?,'CARD','SUCCEEDED',4,'EUR','stub')",
        id,
        order,
        user);
    return id;
  }

  public UUID refund(UUID payment, UUID order) {
    var id = UUID.randomUUID();
    jdbc.update(
        "insert into refunds(id,payment_id,order_id,amount,currency,status) values (?,?,?,4,'EUR','SUCCEEDED')",
        id,
        payment,
        order);
    return id;
  }

  public Catalog catalog() {
    var owner = user();
    var business = business(owner);
    var location = location(business);
    var offer = offer(business, location, category());
    return new Catalog(owner, business, location, offer);
  }

  public record Catalog(UUID owner, UUID business, UUID location, UUID offer) {}
}

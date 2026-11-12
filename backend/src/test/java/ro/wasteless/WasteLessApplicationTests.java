package ro.wasteless;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class WasteLessApplicationTests {
    // Real PostgreSQL binaries, not H2 or mocks. Can instead use an isolated external test database.
    static EmbeddedPostgres postgres;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        String url=System.getenv("TEST_DB_URL");
        if(url==null) {
            postgres=EmbeddedPostgres.builder().setPort(0).start();
            url=postgres.getJdbcUrl("postgres","postgres");
        }
        String finalUrl=url;
        registry.add("spring.datasource.url",()->finalUrl);
        registry.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_DB_USERNAME","postgres"));
        registry.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_DB_PASSWORD",""));
        registry.add("app.jwt.secret",()->"test-only-secret-0123456789-0123456789-0123456789");
    }
    @AfterAll static void closeDatabase() throws Exception { if(postgres!=null) postgres.close(); }
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired JwtEncoder encoder;
    final HttpClient http=HttpClient.newHttpClient();
    record Response(int status,JsonNode body) {}
    record Account(String token,String id,String email) {}
    Response request(String method,String path,String token,Object payload) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path));
        if(token!=null) builder.header("Authorization","Bearer "+token);
        if(payload==null) builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(),json.readTree(response.body()));
    }
    Account register(String role) throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        var r=request("POST","/auth/register",null,Map.of("email",email,"password","Password123!","role",role,
            "businessName","Bakery","pickupLocation","Cluj, Main Street 1"));
        assertThat(r.status).isEqualTo(201);
        assertThat(r.body.has("password")).isFalse();
        assertThat(r.body.get("user").has("passwordHash")).isFalse();
        return new Account(r.body.get("accessToken").asText(),r.body.get("user").get("id").asText(),email);
    }
    Map<String,Object> offerData(int quantity) {
        Map<String,Object> data=new HashMap<>();
        data.put("productName","Croissant rescue bag"); data.put("category","Bakery");
        data.put("description","Fresh croissants"); data.put("originalPrice",8);
        data.put("wasteLessPrice",3.20); data.put("quantity",quantity);
        data.put("pickupStart",OffsetDateTime.now(ZoneOffset.ofHours(2)).plusHours(1).toString());
        data.put("pickupEnd",OffsetDateTime.now(ZoneOffset.ofHours(2)).plusHours(3).toString());
        return data;
    }
    String publish(Account business,int quantity) throws Exception {
        var r=request("POST","/offers",business.token,offerData(quantity));
        assertThat(r.status).isEqualTo(201);
        assertThat(r.body.get("currency").asText()).isEqualTo("EUR");
        assertThat(r.body.get("businessId").asText()).isEqualTo(business.id);
        assertThat(r.body.get("pickupLocation").asText()).isEqualTo("Cluj, Main Street 1");
        return r.body.get("id").asText();
    }
    Response reserve(Account client,String offer,int quantity) throws Exception {
        return request("POST","/reservations",client.token,Map.of("offerId",offer,"quantity",quantity));
    }
    @Test void signupAndLoginBothRolesAndBcrypt() throws Exception {
        for(String role:List.of("CLIENT","BUSINESS")) {
            Account account=register(role);
            var login=request("POST","/auth/login",null,Map.of("email","  "+account.email.toUpperCase(Locale.ROOT)+"  ","password","Password123!"));
            assertThat(login.status).isEqualTo(200);
            assertThat(login.body.get("user").get("role").asText()).isEqualTo(role);
            assertThat(Instant.parse(login.body.get("expiresAt").asText())).isAfter(Instant.now());
            assertThat(db.queryForObject("select password_hash from user_account where id=?::uuid",String.class,account.id)).startsWith("$2");
        }
    }
    @Test void duplicateInvalidLoginAndRegistrationValidation() throws Exception {
        Account account=register("CLIENT");
        assertThat(request("POST","/auth/register",null,Map.of("email",account.email.toUpperCase(Locale.ROOT),"password","Password123!","role","CLIENT")).status).isEqualTo(409);
        assertThat(request("POST","/auth/login",null,Map.of("email",account.email,"password","wrongpassword")).status).isEqualTo(401);
        assertThat(request("POST","/auth/login",null,Map.of("email","unknown@example.com","password","wrongpassword")).status).isEqualTo(401);
        assertThat(request("POST","/auth/register",null,Map.of("email","invalid","password","short","role","CLIENT")).status).isEqualTo(400);
        assertThat(request("POST","/auth/register",null,Map.of("email","b@example.com","password","Password123!","role","BUSINESS")).status).isEqualTo(400);
        assertThat(request("POST","/auth/register",null,Map.of("email","a@example.com","password","Password123!","role","ADMIN")).status).isEqualTo(400);
    }
    @Test void absentTamperedExpiredAndWrongIssuerJwt() throws Exception {
        Account client=register("CLIENT");
        assertThat(request("GET","/offers",null,null).status).isEqualTo(401);
        assertThat(request("GET","/offers","not.a.jwt",null).status).isEqualTo(401);
        String[] parts=client.token.split("\\.");
        // Change signed content, rather than unused base64 padding bits.
        String tampered=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS384\"}".getBytes())
            +"."+parts[1]+"."+parts[2];
        assertThat(request("GET","/offers",tampered,null).status).isEqualTo(401);
        String badSignature=parts[0]+"."+parts[1]+"."+(parts[2].startsWith("A") ? "B" : "A")+parts[2].substring(1);
        assertThat(request("GET","/offers",badSignature,null).status).isEqualTo(401);
        for(boolean expired:List.of(true,false)) {
            Instant now=Instant.now();
            var claims=JwtClaimsSet.builder().issuer(expired?"wasteless":"other").subject(client.id)
                .issuedAt(now.minusSeconds(300)).expiresAt(expired?now.minusSeconds(120):now.plusSeconds(300))
                .claim("role","CLIENT").build();
            String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
            assertThat(request("GET","/offers",token,null).status).isEqualTo(401);
        }
    }
    @Test void rolesAndUnknownFieldsCannotSupplyOwnershipOrPrices() throws Exception {
        Account client=register("CLIENT"), business=register("BUSINESS");
        assertThat(request("POST","/offers",client.token,offerData(1)).status).isEqualTo(403);
        String id=publish(business,2);
        assertThat(reserve(business,id,1).status).isEqualTo(403);
        assertThat(request("GET","/business/offers",client.token,null).status).isEqualTo(403);
        assertThat(request("GET","/offers",business.token,null).status).isEqualTo(403);
        assertThat(request("POST","/reservations",client.token,Map.of("offerId",id,"quantity",1,"total",0,"clientId",business.id)).status).isEqualTo(400);
        Map<String,Object> data=offerData(1); data.put("businessId",client.id);
        assertThat(request("POST","/offers",business.token,data).status).isEqualTo(400);
    }
    @Test void offerValidation() throws Exception {
        Account business=register("BUSINESS");
        for(var entry:Map.<String,Object>of("quantity",0,"originalPrice",-1,"wasteLessPrice",9,
            "productName"," ","category","Unknown","currency","ZZZ","imageUrl","assets/image.jpg").entrySet()) {
            var data=offerData(1); data.put(entry.getKey(),entry.getValue());
            assertThat(request("POST","/offers",business.token,data).status).as(entry.getKey()).isEqualTo(400);
        }
        for(var price:List.of("0","0.001","10000000000")) {
            var data=offerData(1); data.put("wasteLessPrice",price);
            assertThat(request("POST","/offers",business.token,data).status).isEqualTo(400);
        }
        var fractional=offerData(1); fractional.put("quantity",1.5);
        assertThat(request("POST","/offers",business.token,fractional).status).isEqualTo(400);
        var numericCategory=offerData(1); numericCategory.put("category",0);
        assertThat(request("POST","/offers",business.token,numericCategory).status).isEqualTo(400);
        var data=offerData(1); data.put("pickupEnd",data.get("pickupStart"));
        assertThat(request("POST","/offers",business.token,data).status).isEqualTo(400);
        data=offerData(1); data.put("pickupStart","2020-01-01T10:00:00Z"); data.put("pickupEnd","2020-01-01T11:00:00Z");
        assertThat(request("POST","/offers",business.token,data).status).isEqualTo(400);
        data=offerData(1); data.put("pickupStart","2027-01-01T10:00:00");
        assertThat(request("POST","/offers",business.token,data).status).isEqualTo(400);
    }
    @Test void reservationSnapshotsAndStock() throws Exception {
        Account client=register("CLIENT"), business=register("BUSINESS");
        String offer=publish(business,3);
        var r=reserve(client,offer,2);
        assertThat(r.status).isEqualTo(201);
        assertThat(r.body.get("status").asText()).isEqualTo("RESERVED");
        assertThat(r.body.get("unitPrice").decimalValue()).isEqualByComparingTo("3.20");
        assertThat(r.body.get("total").decimalValue()).isEqualByComparingTo("6.40");
        assertThat(r.body.get("currency").asText()).isEqualTo("EUR");
        assertThat(request("GET","/offers/"+offer,client.token,null).body.get("quantity").asInt()).isEqualTo(1);
        db.update("update offer set waste_less_price=4 where id=?::uuid",offer);
        var saved=request("GET","/reservations/"+r.body.get("id").asText(),client.token,null);
        assertThat(saved.body.get("unitPrice").decimalValue()).isEqualByComparingTo("3.20");
    }
    @Test void insufficientExpiredMissingAndInvalidReservation() throws Exception {
        Account client=register("CLIENT"), business=register("BUSINESS");
        String offer=publish(business,1);
        assertThat(reserve(client,offer,2).status).isEqualTo(409);
        assertThat(reserve(client,offer,0).status).isEqualTo(400);
        assertThat(request("POST","/reservations",client.token,Map.of("offerId",offer,"quantity",1.5)).status).isEqualTo(400);
        assertThat(reserve(client,UUID.randomUUID().toString(),1).status).isEqualTo(404);
        assertThat(request("GET","/offers/"+offer,client.token,null).body.get("quantity").asInt()).isEqualTo(1);
        db.update("update offer set pickup_start=now()-interval '2 hours',pickup_end=now()-interval '1 hour' where id=?::uuid",offer);
        assertThat(reserve(client,offer,1).status).isEqualTo(409);
        assertThat(request("GET","/offers/"+offer,client.token,null).body.get("quantity").asInt()).isEqualTo(1);
    }
    @Test void clientAndBusinessIsolation() throws Exception {
        Account a=register("CLIENT"), b=register("CLIENT"), x=register("BUSINESS"), y=register("BUSINESS");
        String offer=publish(x,2), other=publish(y,2);
        var reservation=reserve(a,offer,1);
        String id=reservation.body.get("id").asText();
        assertThat(request("GET","/reservations/"+id,a.token,null).status).isEqualTo(200);
        assertThat(request("GET","/reservations/"+id,x.token,null).status).isEqualTo(200);
        assertThat(request("GET","/reservations/"+id,b.token,null).status).isEqualTo(403);
        assertThat(request("GET","/reservations/"+id,y.token,null).status).isEqualTo(403);
        assertThat(request("GET","/reservations/me",b.token,null).body.get("totalElements").asLong()).isZero();
        assertThat(request("GET","/business/reservations",y.token,null).body.get("totalElements").asLong()).isZero();
        assertThat(request("GET","/business/reservations",x.token,null).body.get("totalElements").asLong()).isEqualTo(1);
        assertThat(request("GET","/reservations/me",a.token,null).body.get("totalElements").asLong()).isEqualTo(1);
        assertThat(request("GET","/business/offers",y.token,null).body.get("content").get(0).get("id").asText()).isEqualTo(other);
        assertThat(request("GET","/offers/"+offer,y.token,null).status).isEqualTo(403);
        assertThat(request("GET","/reservations/"+UUID.randomUUID(),a.token,null).status).isEqualTo(404);
    }
    @Test void availabilityAndPagination() throws Exception {
        Account client=register("CLIENT"), business=register("BUSINESS");
        String sold=publish(business,1), expired=publish(business,1), available=publish(business,1);
        reserve(client,sold,1);
        db.update("update offer set pickup_start=now()-interval '2 hours',pickup_end=now()-interval '1 hour' where id=?::uuid",expired);
        var response=request("GET","/offers?size=100",client.token,null);
        assertThat(response.status).isEqualTo(200);
        List<String> ids=new ArrayList<>();
        response.body.get("content").forEach(n->ids.add(n.get("id").asText()));
        assertThat(ids).contains(available).doesNotContain(sold,expired);
        assertThat(request("GET","/offers?size=1",client.token,null).body.get("content").size()).isEqualTo(1);
        for(String query:List.of("?size=101","?size=0","?page=-1","?page=no")) {
            assertThat(request("GET","/offers"+query,client.token,null).status).isEqualTo(400);
        }
    }
    @Test void failedReservationInsertRollsBackStock() throws Exception {
        Account business=register("BUSINESS"), client=register("CLIENT");
        String offer=publish(business,1);
        // Deliberately reject this insert in PostgreSQL to verify rollback across both writes.
        db.execute("alter table reservation add constraint test_reject_offer check (offer_id <> '"+offer+"'::uuid)");
        try {
            assertThat(reserve(client,offer,1).status).isEqualTo(409);
            assertThat(db.queryForObject("select quantity from offer where id=?::uuid",Integer.class,offer)).isEqualTo(1);
            assertThat(db.queryForObject("select count(*) from reservation where offer_id=?::uuid",Integer.class,offer)).isZero();
        } finally { db.execute("alter table reservation drop constraint test_reject_offer"); }
    }
    @Test void exactlyOneConcurrentReservationForLastUnitOnPostgres() throws Exception {
        Account business=register("BUSINESS"), a=register("CLIENT"), b=register("CLIENT");
        String offer=publish(business,1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
        try {
            List<Future<Response>> futures=new ArrayList<>();
            for(Account client:List.of(a,b)) futures.add(pool.submit(()-> {
                ready.countDown(); if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return reserve(client,offer,1);
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            List<Integer> statuses=List.of(futures.get(0).get(20,TimeUnit.SECONDS).status,futures.get(1).get(20,TimeUnit.SECONDS).status);
            assertThat(statuses).containsExactlyInAnyOrder(201,409);
            assertThat(db.queryForObject("select quantity from offer where id=?::uuid",Integer.class,offer)).isZero();
            assertThat(db.queryForObject("select count(*) from reservation where offer_id=?::uuid",Integer.class,offer)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}

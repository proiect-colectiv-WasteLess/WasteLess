package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.support.*;

class SearchIndexesIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;

  @Test
  @Transactional
  void plannerUsesGeoAndTrigramIndexesAtTenThousandOffers() {
    var f = new Fixtures(jdbc);
    var c = f.catalog();
    var category = f.category();
    jdbc.update(
        "insert into business_locations(business_id,address_line1,city,country_code,latitude,longitude) select ?,'Benchmark','Benchmark','RO',-80+(g%160),-170+(g%340) from generate_series(1,10000) g",
        c.business());
    jdbc.update(
        "insert into offers(business_id,location_id,category_id,title,description,original_price,price,quantity_total,quantity_available,pickup_start_at,pickup_end_at) select ?,?,?,'Benchmark '||g,'Synthetic dataset',12,4,10,10,now(),now()+interval '2 hours' from generate_series(1,9999) g",
        c.business(),
        c.location(),
        category);
    jdbc.update("update offers set title='RareNeedle7a' where id=?", c.offer());
    // Bulk inserts accumulate a GIN pending list; flush it as autovacuum would after loading.
    jdbc.queryForObject("select gin_clean_pending_list('ix_offer_title_search')", Long.class);
    jdbc.execute("analyze offers");
    jdbc.execute("analyze business_locations");
    var geo =
        String.join(
            "\n",
            jdbc.queryForList(
                "explain (analyze,buffers) select id from business_locations where ST_DWithin(location, ST_SetSRID(ST_MakePoint(26.1025,44.4268),4326)::geography,1000)",
                String.class));
    var text =
        String.join(
            "\n",
            jdbc.queryForList(
                "explain (analyze,buffers) select id from offers where title ilike '%RareNeedle7a%'",
                String.class));
    System.out.println("GEO QUERY PLAN (10k locations):\n" + geo);
    System.out.println("TEXT QUERY PLAN (10k offers):\n" + text);
    assertThat(geo).contains("ix_location_geo");
    assertThat(text).contains("ix_offer_title_search");
  }
}

package com.marius.ptr.app.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("integration")
public class InventoryRepositoryJdbcTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void whenReorderThresholdBreachedThenBookIsReportedAsLowStock() {
        jdbcAggregateTemplate.deleteAll(Inventory.class);
        jdbcAggregateTemplate.deleteAll(Book.class);

        var book = jdbcAggregateTemplate.insert(Book.of("1234567890", "Title", "Author", 12.90, "Polarsophia"));
        jdbcAggregateTemplate.insert(Inventory.of(book.isbn(), 2, "WH-EAST-01", 5));

        var lowStock = inventoryRepository.findLowStock();

        assertThat(lowStock).extracting(Inventory::isbn).containsExactly(book.isbn());
        assertThat(lowStock.get(0).isLowStock()).isTrue();
    }
}

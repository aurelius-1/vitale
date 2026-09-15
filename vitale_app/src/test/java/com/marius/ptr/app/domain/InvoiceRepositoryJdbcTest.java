package com.marius.ptr.app.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("integration")
public class InvoiceRepositoryJdbcTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void findInvoiceByNumberWithLinesAndComputedTotal() {
        jdbcAggregateTemplate.deleteAll(Invoice.class);

        var lines = List.of(new InvoiceLine(null, "1234567890", "Title", 2, 12.90));
        jdbcAggregateTemplate.insert(Invoice.issue("INV-1", lines));

        Optional<Invoice> actual = invoiceRepository.findByInvoiceNumber("INV-1");

        assertThat(actual).isPresent();
        assertThat(actual.get().status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(actual.get().lines()).hasSize(1);
        assertThat(actual.get().totalAmount()).isEqualTo(25.80);
    }
}

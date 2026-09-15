package com.marius.ptr.app.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("integration")
public class ShipmentRepositoryJdbcTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void findShipmentsByInvoiceNumber() {
        jdbcAggregateTemplate.deleteAll(Shipment.class);
        jdbcAggregateTemplate.deleteAll(Invoice.class);

        var invoice = jdbcAggregateTemplate.insert(
                Invoice.issue("INV-42", List.of(new InvoiceLine(null, "1234567890", "Title", 1, 12.90))));
        jdbcAggregateTemplate.insert(
                Shipment.create("TRK-1", invoice.invoiceNumber(), "DHL", "WH-EAST-01", "Str. Exemplu 1, Cluj"));

        var shipments = shipmentRepository.findByInvoiceNumber("INV-42");

        assertThat(shipments).hasSize(1);
        assertThat(shipments.get(0).status()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(shipments.get(0).trackingNumber()).isEqualTo("TRK-1");
    }
}

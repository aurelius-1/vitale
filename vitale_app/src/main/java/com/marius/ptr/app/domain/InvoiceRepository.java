package com.marius.ptr.app.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InvoiceRepository extends CrudRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}

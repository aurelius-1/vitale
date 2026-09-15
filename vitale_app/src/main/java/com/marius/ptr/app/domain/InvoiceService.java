package com.marius.ptr.app.domain;

import com.marius.ptr.app.exceptions.BookNotFoundException;
import com.marius.ptr.app.exceptions.InvalidInvoiceStateException;
import com.marius.ptr.app.exceptions.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookRepository bookRepository;
    private final InventoryService inventoryService;

    public InvoiceService(InvoiceRepository invoiceRepository, BookRepository bookRepository, InventoryService inventoryService) {
        this.invoiceRepository = invoiceRepository;
        this.bookRepository = bookRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Invoice issueInvoice(List<InvoiceLineRequest> items) {
        var lines = items.stream()
                .map(item -> {
                    var book = bookRepository.findByIsbn(item.isbn())
                            .orElseThrow(() -> new BookNotFoundException(item.isbn()));
                    inventoryService.reserveStock(item.isbn(), item.quantity());
                    return new InvoiceLine(null, book.isbn(), book.title(), item.quantity(), book.price());
                })
                .toList();

        return invoiceRepository.save(Invoice.issue(generateInvoiceNumber(), lines));
    }

    public Invoice findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
    }

    public Iterable<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public Invoice markAsPaid(String invoiceNumber) {
        var invoice = findByInvoiceNumber(invoiceNumber);
        if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new InvalidInvoiceStateException(invoiceNumber, invoice.status(), "pay");
        }
        return invoiceRepository.save(invoice.paid());
    }

    @Transactional
    public Invoice cancel(String invoiceNumber) {
        var invoice = findByInvoiceNumber(invoiceNumber);
        if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new InvalidInvoiceStateException(invoiceNumber, invoice.status(), "cancel");
        }
        invoice.lines().forEach(line -> inventoryService.releaseStock(line.isbn(), line.quantity()));
        return invoiceRepository.save(invoice.cancelled());
    }

    private String generateInvoiceNumber() {
        return "INV-" + Instant.now().toEpochMilli();
    }
}

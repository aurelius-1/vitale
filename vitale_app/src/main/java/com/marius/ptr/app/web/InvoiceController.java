package com.marius.ptr.app.web;

import com.marius.ptr.app.domain.Invoice;
import com.marius.ptr.app.domain.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public Iterable<Invoice> getAll() {
        return invoiceService.findAll();
    }

    @GetMapping("{invoiceNumber}")
    public Invoice getByNumber(@PathVariable String invoiceNumber) {
        return invoiceService.findByInvoiceNumber(invoiceNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice issue(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.issueInvoice(request.lines());
    }

    @PostMapping("{invoiceNumber}/pay")
    public Invoice pay(@PathVariable String invoiceNumber) {
        return invoiceService.markAsPaid(invoiceNumber);
    }

    @PostMapping("{invoiceNumber}/cancel")
    public Invoice cancel(@PathVariable String invoiceNumber) {
        return invoiceService.cancel(invoiceNumber);
    }
}

package com.marius.ptr.app.web;

import com.marius.ptr.app.domain.Inventory;
import com.marius.ptr.app.domain.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("low-stock")
    public List<Inventory> lowStock() {
        return inventoryService.findLowStock();
    }

    @GetMapping("{isbn}")
    public Inventory getStock(@PathVariable String isbn) {
        return inventoryService.getStock(isbn);
    }

    @PostMapping("{isbn}/receive")
    public Inventory receive(@PathVariable String isbn, @Valid @RequestBody ReceiveStockRequest request) {
        return inventoryService.receiveStock(isbn, request.quantity(), request.warehouseLocation());
    }

    @PostMapping("{isbn}/reserve")
    public Inventory reserve(@PathVariable String isbn, @Valid @RequestBody StockQuantityRequest request) {
        return inventoryService.reserveStock(isbn, request.quantity());
    }

    @PostMapping("{isbn}/release")
    public Inventory release(@PathVariable String isbn, @Valid @RequestBody StockQuantityRequest request) {
        return inventoryService.releaseStock(isbn, request.quantity());
    }

    public record ReceiveStockRequest(
            @Min(value = 1, message = "The quantity must be at least 1")
            int quantity,

            @NotBlank(message = "The warehouse location must be defined")
            String warehouseLocation
    ) {}

    public record StockQuantityRequest(
            @Min(value = 1, message = "The quantity must be at least 1")
            int quantity
    ) {}
}

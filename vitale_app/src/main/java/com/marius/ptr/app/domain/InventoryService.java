package com.marius.ptr.app.domain;

import com.marius.ptr.app.exceptions.BookNotFoundException;
import com.marius.ptr.app.exceptions.InsufficientStockException;
import com.marius.ptr.app.exceptions.InventoryNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private static final int DEFAULT_REORDER_THRESHOLD = 5;

    private final InventoryRepository inventoryRepository;
    private final BookRepository bookRepository;

    public InventoryService(InventoryRepository inventoryRepository, BookRepository bookRepository) {
        this.inventoryRepository = inventoryRepository;
        this.bookRepository = bookRepository;
    }

    public Inventory getStock(String isbn) {
        return inventoryRepository.findById(isbn)
                .orElseThrow(() -> new InventoryNotFoundException(isbn));
    }

    public List<Inventory> findLowStock() {
        return inventoryRepository.findLowStock();
    }

    public Inventory receiveStock(String isbn, int quantity, String warehouseLocation) {
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException(isbn);
        }

        return inventoryRepository.findById(isbn)
                .map(inventory -> inventoryRepository.save(inventory.withQuantityOnHand(inventory.quantityOnHand() + quantity)))
                .orElseGet(() -> inventoryRepository.save(Inventory.of(isbn, quantity, warehouseLocation, DEFAULT_REORDER_THRESHOLD)));
    }

    public Inventory reserveStock(String isbn, int quantity) {
        var inventory = getStock(isbn);
        if (inventory.quantityOnHand() < quantity) {
            throw new InsufficientStockException(isbn, quantity, inventory.quantityOnHand());
        }
        return inventoryRepository.save(inventory.withQuantityOnHand(inventory.quantityOnHand() - quantity));
    }

    public Inventory releaseStock(String isbn, int quantity) {
        var inventory = getStock(isbn);
        return inventoryRepository.save(inventory.withQuantityOnHand(inventory.quantityOnHand() + quantity));
    }
}

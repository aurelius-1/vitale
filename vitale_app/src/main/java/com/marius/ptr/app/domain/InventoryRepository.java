package com.marius.ptr.app.domain;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface InventoryRepository extends CrudRepository<Inventory, String> {

    @Query("SELECT * FROM inventory WHERE quantity_on_hand <= reorder_threshold")
    List<Inventory> findLowStock();
}

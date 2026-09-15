CREATE TABLE inventory (
    isbn                varchar(255) PRIMARY KEY REFERENCES book (isbn),
    quantity_on_hand    integer NOT NULL DEFAULT 0,
    warehouse_location  varchar(255) NOT NULL,
    reorder_threshold   integer NOT NULL DEFAULT 5,
    created_date        timestamp NOT NULL,
    last_modified_date  timestamp NOT NULL,
    version             integer NOT NULL
);

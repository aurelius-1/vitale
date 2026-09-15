CREATE TABLE shipment (
    id                   BIGSERIAL PRIMARY KEY,
    tracking_number      varchar(255) UNIQUE NOT NULL,
    invoice_number       varchar(255) NOT NULL REFERENCES invoice (invoice_number),
    carrier              varchar(255) NOT NULL,
    status               varchar(50) NOT NULL,
    origin_warehouse     varchar(255) NOT NULL,
    destination_address  varchar(255) NOT NULL,
    shipped_date         timestamp,
    delivered_date       timestamp,
    created_date         timestamp NOT NULL,
    last_modified_date   timestamp NOT NULL,
    version              integer NOT NULL
);

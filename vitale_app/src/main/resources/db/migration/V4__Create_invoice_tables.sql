CREATE TABLE invoice (
    id                  BIGSERIAL PRIMARY KEY,
    invoice_number      varchar(255) UNIQUE NOT NULL,
    status              varchar(50) NOT NULL,
    total_amount        float(8) NOT NULL,
    issued_date         timestamp NOT NULL,
    paid_date           timestamp,
    created_date        timestamp NOT NULL,
    last_modified_date  timestamp NOT NULL,
    version             integer NOT NULL
);

CREATE TABLE invoice_line (
    id          BIGSERIAL PRIMARY KEY,
    invoice_id  bigint NOT NULL REFERENCES invoice (id) ON DELETE CASCADE,
    isbn        varchar(255) NOT NULL,
    title       varchar(255) NOT NULL,
    quantity    integer NOT NULL,
    unit_price  float(8) NOT NULL
);

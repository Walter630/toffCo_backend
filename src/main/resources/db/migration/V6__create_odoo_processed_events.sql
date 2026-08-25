CREATE TABLE IF NOT EXISTS odoo_processed_events
(
    id                UUID         NOT NULL PRIMARY KEY,
    odoo_move_line_id BIGINT       NOT NULL,
    product_barcode   VARCHAR(255),
    status            VARCHAR(20)  NOT NULL,
    error_message     VARCHAR(255),
    processed_at      TIMESTAMP,

    CONSTRAINT uk_odoo_processed_move_line
        UNIQUE (odoo_move_line_id)
);

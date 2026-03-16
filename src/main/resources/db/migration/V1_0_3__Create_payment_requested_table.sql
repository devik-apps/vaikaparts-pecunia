CREATE TABLE payment_requested
(
    id             VARCHAR(45) PRIMARY KEY,
    payment_id     VARCHAR(45)  NOT NULL REFERENCES payments (id),
    transaction_id VARCHAR(50)  NOT NULL,
    status         event_status NOT NULL DEFAULT 'PENDING'
);
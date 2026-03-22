CREATE TABLE mvola_payments
(
    id                    VARCHAR(45) PRIMARY KEY,
    server_correlation_id VARCHAR(255),
    mvola_transaction_id  VARCHAR(255),
    notification_method   VARCHAR(50),

    CONSTRAINT fk_mvola_payments_payments FOREIGN KEY (id) REFERENCES payments (id)
);
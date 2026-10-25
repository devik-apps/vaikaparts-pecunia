ALTER TABLE payments ALTER COLUMN transaction_id TYPE VARCHAR(255);

CREATE TABLE papi_payments
(
    id                     VARCHAR(45) PRIMARY KEY,
    papi_payment_reference VARCHAR(255),
    merchant_reference     VARCHAR(255) NOT NULL UNIQUE,
    payment_method         VARCHAR(32)  NOT NULL,
    notification_token     VARCHAR(255),

    CONSTRAINT fk_papi_payments_payments FOREIGN KEY (id) REFERENCES payments (id)
);

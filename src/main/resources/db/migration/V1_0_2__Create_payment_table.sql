CREATE TABLE payments
(
    id             VARCHAR(45) PRIMARY KEY,
    transaction_id VARCHAR(50),
    description    TEXT                     NOT NULL,
    payer_id       VARCHAR(45) REFERENCES payment_parties (id),
    payee_id       VARCHAR(45) REFERENCES payment_parties (id),
    provider       payment_provider         NOT NULL,
    type           payment_type             NOT NULL,
    amount         float8                   NOT NULL,
    currency       payment_currency         NOT NULL DEFAULT 'AR',
    status         verification_status               DEFAULT 'PENDING',
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
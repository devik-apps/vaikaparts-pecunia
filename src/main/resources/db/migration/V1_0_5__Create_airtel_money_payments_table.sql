CREATE TABLE airtel_money_payments
(
    id              VARCHAR(45) PRIMARY KEY,
    airtel_money_id VARCHAR(25),

    CONSTRAINT fk_airtel_money_payments_payments FOREIGN KEY (id) REFERENCES payments (id)
);
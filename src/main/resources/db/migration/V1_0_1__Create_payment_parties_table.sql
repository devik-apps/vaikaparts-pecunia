CREATE TABLE payment_parties
(
    id           VARCHAR(45) PRIMARY KEY,
    phone_number VARCHAR(20)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    country      country      NOT NULL DEFAULT 'MADAGASCAR'
);
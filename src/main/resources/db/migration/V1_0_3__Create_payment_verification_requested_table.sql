CREATE TABLE payment_verification_requested
(
    id                         VARCHAR(45) PRIMARY KEY,
    payment_id                 VARCHAR(45)         NOT NULL REFERENCES payments (id),
    status                     verification_status NOT NULL DEFAULT 'PENDING',
    attempt_nb                 INT                 NOT NULL,
    max_verification_attemp_nb INT                 NOT NULL DEFAULT 3,
    error_message              TEXT,
    created_at                 TIMESTAMP WITH TIME ZONE     DEFAULT current_timestamp,
    last_verified_at           TIMESTAMP WITH TIME ZONE,
    completed_at               TIMESTAMP WITH TIME ZONE
);
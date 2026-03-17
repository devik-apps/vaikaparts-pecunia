CREATE TYPE payment_provider AS ENUM ('MVOLA');

CREATE TYPE payment_type AS ENUM ('PROFILE_UNLOCK');

CREATE TYPE payment_currency AS ENUM ('AR');

CREATE TYPE country AS ENUM ('MADAGASCAR');

CREATE TYPE verification_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
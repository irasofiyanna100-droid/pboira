CREATE DATABASE IF NOT EXISTS koperasi_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE koperasi_db;

CREATE TABLE IF NOT EXISTS koperasi_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    username VARCHAR(30) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    proof_path VARCHAR(1024) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    nik VARCHAR(16) NOT NULL,
    address VARCHAR(1000) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS koperasi_settings (
    singleton_id INTEGER NOT NULL PRIMARY KEY,
    cooperative_name VARCHAR(200) NOT NULL,
    annual_interest_percent DOUBLE NOT NULL,
    late_penalty_percent DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS koperasi_savings (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    member_name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    transaction_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS koperasi_loans (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    member_name VARCHAR(200) NOT NULL,
    principal BIGINT NOT NULL,
    purpose VARCHAR(2000) NOT NULL,
    months INTEGER NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    disbursed BOOLEAN NOT NULL,
    total_repayable BIGINT NOT NULL,
    remaining BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS koperasi_installments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    loan_id VARCHAR(36) NOT NULL,
    username VARCHAR(30) NOT NULL,
    member_name VARCHAR(200) NOT NULL,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    amount BIGINT NOT NULL,
    paid BOOLEAN NOT NULL,
    paid_at TIMESTAMP NULL,
    CONSTRAINT fk_koperasi_installments_loan
        FOREIGN KEY (loan_id) REFERENCES koperasi_loans(id)
);

CREATE TABLE IF NOT EXISTS koperasi_notices (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

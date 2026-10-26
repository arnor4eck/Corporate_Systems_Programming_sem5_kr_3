CREATE TABLE customers (
    id UUID PRIMARY KEY,
    archive_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (archive_status IN ('ACTIVE', 'ARCHIVE')),
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE sectors (
    id UUID PRIMARY KEY,
    archive_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (archive_status IN ('ACTIVE', 'ARCHIVE')),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    archive_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (archive_status IN ('ACTIVE', 'ARCHIVE')),
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'WORKER')),
    login VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE plots (
    id UUID PRIMARY KEY,
    archive_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (archive_status IN ('ACTIVE', 'ARCHIVE')),
    sector_id UUID NOT NULL REFERENCES sectors(id) ON DELETE RESTRICT,
    row_number INTEGER NOT NULL,
    plot_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('FREE', 'RESERVED', 'OCCUPIED')),
    length_cm INTEGER NOT NULL,
    width_cm INTEGER NOT NULL,
    coordinates VARCHAR(100),
    CONSTRAINT uk_plot_location UNIQUE (sector_id, row_number, plot_number)
);

CREATE TABLE requests (
    id UUID PRIMARY KEY,
    archive_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (archive_status IN ('ACTIVE', 'ARCHIVE')),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    plot_id UUID NOT NULL REFERENCES plots(id) ON DELETE RESTRICT,
    employee_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    deceased_full_name VARCHAR(150),
    deceased_birth_date DATE,
    deceased_death_date DATE,
    deceased_certificate VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'PROCESSING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    request_date TIMESTAMP NOT NULL,
    processed_date TIMESTAMP,
    total_cost DECIMAL(10,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_plots_status_sector ON plots(sector_id, status);
CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_customer ON requests(customer_id);

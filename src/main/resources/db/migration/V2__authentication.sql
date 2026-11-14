ALTER TABLE employees ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE employees ADD COLUMN failed_logins INTEGER NOT NULL DEFAULT 0;
ALTER TABLE employees ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_refresh_employee ON refresh_sessions(employee_id);

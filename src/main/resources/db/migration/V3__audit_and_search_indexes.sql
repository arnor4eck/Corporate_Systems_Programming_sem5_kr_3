CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_login VARCHAR(50) NOT NULL,
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    description VARCHAR(300) NOT NULL
);
CREATE INDEX idx_audit_time ON audit_events(occurred_at);
CREATE INDEX idx_audit_actor ON audit_events(actor_id);
CREATE INDEX idx_audit_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_requests_date_cost ON requests(request_date, total_cost);
CREATE INDEX idx_requests_employee ON requests(employee_id);
CREATE INDEX idx_requests_plot ON requests(plot_id);
CREATE INDEX idx_customers_name ON customers(full_name);
CREATE INDEX idx_employees_role_active ON employees(role, is_active);
CREATE INDEX idx_sectors_name ON sectors(name);

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;

CREATE TABLE security_audit_logs (
                                     id UUID PRIMARY KEY,
                                     actor_id UUID,
                                     target_id UUID,
                                     action VARCHAR(50) NOT NULL,
                                     details VARCHAR(500),
                                     ip_address VARCHAR(45),
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor_id ON security_audit_logs(actor_id);
CREATE INDEX idx_audit_logs_target_id ON security_audit_logs(target_id);
CREATE INDEX idx_audit_logs_created_at ON security_audit_logs(created_at);
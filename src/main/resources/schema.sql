CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    real_name VARCHAR(64) NOT NULL
);

CREATE TABLE rpt_repair_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    repair_type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reporter_id BIGINT NOT NULL,
    assignee_id BIGINT NULL,
    repair_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    material_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    assigned_at DATETIME NULL,
    completed_at DATETIME NULL,
    verified_at DATETIME NULL,
    CONSTRAINT chk_repair_type CHECK (repair_type IN ('PLUMBING', 'FURNITURE', 'APPLIANCE', 'NETWORK', 'OTHER')),
    CONSTRAINT chk_repair_priority CHECK (priority IN ('URGENT', 'NORMAL', 'LOW')),
    CONSTRAINT chk_repair_status CHECK (status IN ('PENDING', 'PROCESSING', 'WAITING_CHECK', 'COMPLETED')),
    CONSTRAINT chk_repair_fees CHECK (repair_fee >= 0 AND material_fee >= 0 AND total_fee = repair_fee + material_fee),
    CONSTRAINT chk_repair_lifecycle CHECK (
        (status = 'PENDING' AND assignee_id IS NULL AND assigned_at IS NULL AND completed_at IS NULL AND verified_at IS NULL)
        OR (status = 'PROCESSING' AND assignee_id IS NOT NULL AND assigned_at IS NOT NULL AND completed_at IS NULL AND verified_at IS NULL)
        OR (status = 'WAITING_CHECK' AND assignee_id IS NOT NULL AND assigned_at IS NOT NULL AND completed_at IS NOT NULL AND verified_at IS NULL)
        OR (status = 'COMPLETED' AND assignee_id IS NOT NULL AND assigned_at IS NOT NULL AND completed_at IS NOT NULL AND verified_at IS NOT NULL)
    ),
    CONSTRAINT fk_repair_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user(id),
    CONSTRAINT fk_repair_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
);

CREATE TABLE rpt_cleaning_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area VARCHAR(100) NOT NULL,
    cleaner_name VARCHAR(64) NOT NULL,
    plan_date DATE NOT NULL,
    plan_time TIME NULL,
    status VARCHAR(30) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT chk_cleaning_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

CREATE INDEX idx_repair_status_type_created
    ON rpt_repair_order (status, repair_type, created_at);
CREATE INDEX idx_repair_reporter ON rpt_repair_order (reporter_id);
CREATE INDEX idx_repair_assignee_status ON rpt_repair_order (assignee_id, status);
CREATE INDEX idx_cleaning_date_status ON rpt_cleaning_plan (plan_date, status);

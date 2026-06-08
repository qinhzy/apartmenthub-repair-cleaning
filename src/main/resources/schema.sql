DROP TABLE IF EXISTS rpt_repair_order;
DROP TABLE IF EXISTS rpt_cleaning_plan;
DROP TABLE IF EXISTS sys_user;

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
    CONSTRAINT fk_repair_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user(id),
    CONSTRAINT fk_repair_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
);

CREATE TABLE rpt_cleaning_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area VARCHAR(100) NOT NULL,
    cleaner_name VARCHAR(64) NOT NULL,
    plan_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);


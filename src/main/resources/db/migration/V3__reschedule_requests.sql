CREATE TABLE reschedule_request_statuses (
    id SMALLINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE reschedule_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT UNSIGNED NOT NULL,
    requested_by_user_id BIGINT UNSIGNED NOT NULL,
    requested_location_id SMALLINT UNSIGNED NOT NULL,
    status_id SMALLINT UNSIGNED NOT NULL,
    previous_start_at DATETIME NOT NULL,
    previous_end_at DATETIME NOT NULL,
    requested_start_at DATETIME NOT NULL,
    requested_end_at DATETIME NOT NULL,
    decision_reason VARCHAR(500) NULL,
    decided_by_user_id BIGINT UNSIGNED NULL,
    decided_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reschedule_time CHECK (requested_end_at > requested_start_at),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
    FOREIGN KEY (requested_location_id) REFERENCES locations(id),
    FOREIGN KEY (status_id) REFERENCES reschedule_request_statuses(id),
    FOREIGN KEY (decided_by_user_id) REFERENCES users(id),
    INDEX ix_reschedule_appointment_status (appointment_id, status_id),
    INDEX ix_reschedule_status (status_id)
) ENGINE=InnoDB;

INSERT INTO reschedule_request_statuses(id, code, name, is_terminal) VALUES
    (1, 'PENDING', 'Pendiente', FALSE),
    (2, 'APPROVED', 'Aprobada', TRUE),
    (3, 'REJECTED', 'Rechazada', TRUE),
    (4, 'CANCELLED', 'Cancelada por el usuario', TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name), is_terminal=VALUES(is_terminal);

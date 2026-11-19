-- Fresh install. Run with stop-on-error enabled. This script never drops data.
CREATE DATABASE campus_equipment_borrowing
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_equipment_borrowing;

-- 1. TABLE DEFINITIONS

CREATE TABLE USER_ACCOUNT (
    user_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    university_id  VARCHAR(20) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL,
    role           ENUM('STUDENT','STAFF','ADMIN') NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_user_university_id UNIQUE (university_id),
    CONSTRAINT uq_user_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE CATEGORY (
    category_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_name  VARCHAR(80) NOT NULL,
    description    VARCHAR(255) NULL,
    CONSTRAINT uq_category_name UNIQUE (category_name)
) ENGINE = InnoDB;

CREATE TABLE EQUIPMENT_ITEM (
    item_id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id      INT UNSIGNED NOT NULL,
    asset_tag        VARCHAR(30) NOT NULL,
    item_name        VARCHAR(120) NOT NULL,
    location         VARCHAR(100) NOT NULL,
    condition_status ENUM('GOOD','FAIR','DAMAGED','UNDER_REPAIR') NOT NULL DEFAULT 'GOOD',
    lifecycle_status ENUM('ACTIVE','MAINTENANCE','RETIRED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_equipment_asset_tag UNIQUE (asset_tag),
    CONSTRAINT fk_equipment_category FOREIGN KEY (category_id)
        REFERENCES CATEGORY (category_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE BORROW_TRANSACTION (
    transaction_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    borrower_id      INT UNSIGNED NOT NULL,
    processed_by     INT UNSIGNED NULL,
    requested_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    requested_due_at DATETIME NOT NULL,
    released_at      DATETIME NULL,
    due_at           DATETIME NULL,
    status           ENUM('PENDING','APPROVED','REJECTED','CANCELLED','RELEASED','CLOSED')
                     NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_transaction_borrower FOREIGN KEY (borrower_id)
        REFERENCES USER_ACCOUNT (user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_transaction_processor FOREIGN KEY (processed_by)
        REFERENCES USER_ACCOUNT (user_id),
    CONSTRAINT chk_requested_due CHECK (requested_due_at > requested_at),
    CONSTRAINT chk_release_due CHECK (
        (released_at IS NULL AND due_at IS NULL) OR
        (released_at IS NOT NULL AND due_at IS NOT NULL AND due_at > released_at)
    ),
    CONSTRAINT chk_transaction_status CHECK (
        (status = 'PENDING' AND processed_by IS NULL AND released_at IS NULL) OR
        (status = 'CANCELLED' AND released_at IS NULL) OR
        (status IN ('APPROVED','REJECTED') AND processed_by IS NOT NULL
             AND released_at IS NULL) OR
        (status IN ('RELEASED','CLOSED') AND processed_by IS NOT NULL
             AND released_at IS NOT NULL)
    ),
    INDEX idx_transaction_borrower (borrower_id),
    INDEX idx_transaction_processor (processed_by),
    INDEX idx_transaction_status_due (status, due_at)
) ENGINE = InnoDB;

CREATE TABLE BORROW_ITEM (
    transaction_id INT UNSIGNED NOT NULL,
    item_id        INT UNSIGNED NOT NULL,
    condition_out  ENUM('GOOD','FAIR','DAMAGED') NULL,
    returned_at    DATETIME NULL,
    condition_in   ENUM('GOOD','FAIR','DAMAGED') NULL,
    PRIMARY KEY (transaction_id, item_id),
    CONSTRAINT fk_borrow_item_transaction FOREIGN KEY (transaction_id)
        REFERENCES BORROW_TRANSACTION (transaction_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_borrow_item_equipment FOREIGN KEY (item_id)
        REFERENCES EQUIPMENT_ITEM (item_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_return_fields CHECK (
        (returned_at IS NULL AND condition_in IS NULL) OR
        (returned_at IS NOT NULL AND condition_in IS NOT NULL
            AND condition_out IS NOT NULL)
    ),
    INDEX idx_borrow_item_equipment (item_id)
) ENGINE = InnoDB;

CREATE TABLE MAINTENANCE_RECORD (
    record_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    item_id           INT UNSIGNED NOT NULL,
    reported_by       INT UNSIGNED NOT NULL,
    reported_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    issue_description TEXT NOT NULL,
    status            ENUM('OPEN','IN_PROGRESS','RESOLVED') NOT NULL DEFAULT 'OPEN',
    resolved_at       DATETIME NULL,
    CONSTRAINT fk_maintenance_item FOREIGN KEY (item_id)
        REFERENCES EQUIPMENT_ITEM (item_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_maintenance_reporter FOREIGN KEY (reported_by)
        REFERENCES USER_ACCOUNT (user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_maintenance_resolution CHECK (
        (status = 'RESOLVED' AND resolved_at IS NOT NULL AND resolved_at >= reported_at)
        OR (status IN ('OPEN','IN_PROGRESS') AND resolved_at IS NULL)
    ),
    INDEX idx_maintenance_item_status (item_id, status),
    INDEX idx_maintenance_reporter (reported_by)
) ENGINE = InnoDB;

-- 2. INITIAL TEST DATA

INSERT INTO USER_ACCOUNT
    (user_id, university_id, full_name, email, role, is_active)
VALUES
    (1,  '2026-0001', 'Alexander Young',        'alexander.young@gmail.com',        'STUDENT', TRUE),
    (2,  '2026-0002', 'Andre France Judilla',    'andre.france.judilla@gmail.com',   'STUDENT', TRUE),
    (3,  '2026-0003', 'Geoff Miguil Salcedo',   'geoff.miguil.salcedo@gmail.com',   'STUDENT', TRUE),
    (4,  '2026-0004', 'Ivy Caldi',              'ivy.caldi@gmail.com',               'STUDENT', TRUE),
    (5,  '2026-0005', 'Phoenix Paras',          'phoenix.paras@gmail.com',           'STUDENT', TRUE),
    (6,  '2026-0006', 'Reynold Angelo Segundo', 'reynold.angelo.segundo@gmail.com',  'STUDENT', TRUE),
    (7,  'EMP-0101',  'Sofia Villanueva',       'sofia.villanueva@gmail.com',        'STAFF',   TRUE),
    (8,  'EMP-0102',  'Tristan Ramos',          'tristan.ramos@gmail.com',            'STAFF',   TRUE),
    (9,  'ADM-0001',  'Ursula Garcia',          'ursula.garcia@gmail.com',            'ADMIN',   TRUE),
    (10, '2026-0010', 'Xavier Lim',             'xavier.lim@gmail.com',               'STUDENT', FALSE);

INSERT INTO CATEGORY (category_id, category_name, description) VALUES
    (1, 'Cameras',         'Digital cameras and video recording equipment'),
    (2, 'Projectors',      'Classroom and event projection equipment'),
    (3, 'Laptops',         'Portable computers for academic activities'),
    (4, 'Audio Equipment', 'Microphones and portable audio devices'),
    (5, 'Accessories',     'Tripods, adapters, and related accessories');

INSERT INTO EQUIPMENT_ITEM
    (item_id, category_id, asset_tag, item_name, location, condition_status, lifecycle_status)
VALUES
    (1,  1, 'CAM-001',  'Canon DSLR Camera',       'Media Lab',       'GOOD',         'ACTIVE'),
    (2,  5, 'ACC-001',  'Camera Tripod',           'Media Lab',       'GOOD',         'ACTIVE'),
    (3,  2, 'PRO-001',  'Epson Projector',         'AV Room',         'GOOD',         'ACTIVE'),
    (4,  5, 'ACC-002',  'HDMI Adapter',            'AV Room',         'GOOD',         'ACTIVE'),
    (5,  3, 'LAP-001',  'Lenovo ThinkPad Laptop',  'IT Office',       'GOOD',         'ACTIVE'),
    (6,  4, 'AUD-001',  'Wireless Microphone',    'AV Room',         'GOOD',         'ACTIVE'),
    (7,  4, 'AUD-002',  'Portable PA Speaker',    'AV Room',         'GOOD',         'ACTIVE'),
    (8,  2, 'PRO-002',  'BenQ Projector',          'AV Room',         'GOOD',         'ACTIVE'),
    (9,  5, 'ACC-003',  'Projector Screen',        'AV Room',         'GOOD',         'ACTIVE'),
    (10, 1, 'CAM-002',  'Sony Mirrorless Camera', 'Media Lab',       'GOOD',         'ACTIVE'),
    (11, 3, 'LAP-002',  'Dell Laptop',             'IT Office',       'UNDER_REPAIR', 'MAINTENANCE'),
    (12, 4, 'AUD-003',  'Wired Microphone',        'Storage Room',   'DAMAGED',      'RETIRED');

INSERT INTO BORROW_TRANSACTION
    (transaction_id, borrower_id, processed_by, requested_at, requested_due_at,
     released_at, due_at, status)
VALUES
    (1, 1, 7, '2026-09-19 08:00:00', '2026-09-25 17:00:00',
       '2026-09-20 09:00:00', '2026-09-25 17:00:00', 'CLOSED'),
    (2, 2, 8, '2026-09-22 10:00:00', '2026-09-27 17:00:00',
       '2026-09-23 10:30:00', '2026-09-27 17:00:00', 'CLOSED'),
    (3, 3, 7, '2026-10-21 08:00:00', '2026-10-30 17:00:00',
       '2026-10-22 09:00:00', '2026-10-30 17:00:00', 'RELEASED'),
    (4, 4, 8, '2026-10-16 08:00:00', '2026-10-22 17:00:00',
       '2026-10-17 13:00:00', '2026-10-22 17:00:00', 'RELEASED'),
    (5, 5, 7, '2026-10-24 09:00:00', '2026-10-29 17:00:00',
       NULL, NULL, 'APPROVED'),
    (6, 6, NULL, '2026-10-25 08:00:00', '2026-10-28 17:00:00',
       NULL, NULL, 'PENDING'),
    (7, 1, 8, '2026-10-24 11:00:00', '2026-10-28 17:00:00',
       NULL, NULL, 'REJECTED'),
    (8, 2, NULL, '2026-10-20 08:00:00', '2026-10-25 17:00:00',
       NULL, NULL, 'CANCELLED'),
    (9, 5, 7, '2026-09-30 08:00:00', '2026-10-08 17:00:00',
       '2026-10-01 11:00:00', '2026-10-08 17:00:00', 'CLOSED');

INSERT INTO BORROW_ITEM
    (transaction_id, item_id, condition_out, returned_at, condition_in)
VALUES
    (1, 1,  'GOOD', '2026-09-24 16:00:00', 'GOOD'),
    (1, 2,  'GOOD', '2026-09-24 16:00:00', 'GOOD'),
    (2, 3,  'GOOD', '2026-09-26 14:00:00', 'GOOD'),
    (2, 4,  'GOOD', '2026-09-26 14:00:00', 'GOOD'),
    (3, 1,  'GOOD', NULL, NULL),
    (3, 5,  'GOOD', NULL, NULL),
    (4, 6,  'GOOD', NULL, NULL),
    (4, 7,  'GOOD', NULL, NULL),
    (5, 8,  NULL, NULL, NULL),
    (5, 9,  NULL, NULL, NULL),
    (6, 10, NULL, NULL, NULL),
    (7, 11, NULL, NULL, NULL),
    (8, 12, NULL, NULL, NULL),
    (9, 5,  'GOOD', '2026-10-07 13:00:00', 'GOOD'),
    (9, 7,  'GOOD', '2026-10-07 13:00:00', 'GOOD');

INSERT INTO MAINTENANCE_RECORD
    (record_id, item_id, reported_by, reported_at, issue_description, status, resolved_at)
VALUES
    (1, 11, 7, '2026-10-15 09:00:00', 'Laptop does not boot reliably.',
       'IN_PROGRESS', NULL),
    (2, 12, 8, '2026-10-12 10:00:00', 'Cable is broken and item was retired.',
       'OPEN', NULL),
    (3, 3,  7, '2026-09-12 11:00:00', 'Projector filter needed cleaning.',
       'RESOLVED', '2026-09-13 14:00:00'),
    (4, 2,  8, '2026-09-10 11:00:00', 'Tripod leg was loose and was tightened.',
       'RESOLVED', '2026-09-11 15:00:00');

-- 3. WEEK 4 REPORT VIEWS

-- Available equipment
CREATE VIEW v_available_equipment AS
SELECT e.asset_tag, e.item_name, c.category_name,
      e.location, e.condition_status
 FROM EQUIPMENT_ITEM AS e
 JOIN CATEGORY AS c ON e.category_id = c.category_id
 WHERE e.lifecycle_status = 'ACTIVE'
  AND e.condition_status = 'GOOD'
  AND NOT EXISTS (
     SELECT 1 FROM BORROW_ITEM AS bi
     JOIN BORROW_TRANSACTION AS t ON bi.transaction_id = t.transaction_id
     WHERE bi.item_id = e.item_id AND t.status = 'RELEASED'
      AND bi.returned_at IS NULL
  )
  AND NOT EXISTS (
     SELECT 1 FROM MAINTENANCE_RECORD AS m
     WHERE m.item_id = e.item_id AND m.status IN ('OPEN', 'IN_PROGRESS')
  );

-- Currently borrowed equipment
CREATE VIEW v_currently_borrowed AS
SELECT t.transaction_id, u.full_name AS borrower, e.asset_tag,
     e.item_name, t.released_at, t.due_at
 FROM BORROW_TRANSACTION AS t
 JOIN USER_ACCOUNT AS u ON t.borrower_id = u.user_id
 JOIN BORROW_ITEM AS bi ON t.transaction_id = bi.transaction_id
 JOIN EQUIPMENT_ITEM AS e ON bi.item_id = e.item_id
 WHERE t.status = 'RELEASED' AND bi.returned_at IS NULL;

-- Overdue equipment
CREATE VIEW v_overdue_equipment AS
SELECT t.transaction_id, u.full_name AS borrower, u.university_id,
     e.asset_tag, e.item_name, t.due_at
 FROM BORROW_TRANSACTION AS t
 JOIN USER_ACCOUNT AS u ON t.borrower_id = u.user_id
 JOIN BORROW_ITEM AS bi ON t.transaction_id = bi.transaction_id
 JOIN EQUIPMENT_ITEM AS e ON bi.item_id = e.item_id
 WHERE t.status = 'RELEASED' AND bi.returned_at IS NULL
  AND t.due_at < NOW();

-- Pending borrowing requests
CREATE VIEW v_pending_requests AS
SELECT t.transaction_id, u.university_id, u.full_name AS borrower,
     t.requested_at, t.requested_due_at, t.status
 FROM BORROW_TRANSACTION AS t
 JOIN USER_ACCOUNT AS u ON t.borrower_id = u.user_id
 WHERE t.status = 'PENDING';

-- Requests per user
CREATE VIEW v_requests_per_user AS
SELECT u.user_id, u.full_name, u.role,
    COUNT(t.transaction_id) AS total_requests
 FROM USER_ACCOUNT AS u
 LEFT JOIN BORROW_TRANSACTION AS t ON u.user_id = t.borrower_id
 GROUP BY u.user_id, u.full_name, u.role;

-- Equipment never released
CREATE VIEW v_equipment_never_released AS
SELECT e.asset_tag, e.item_name, c.category_name, e.lifecycle_status
 FROM EQUIPMENT_ITEM AS e
 JOIN CATEGORY AS c ON e.category_id = c.category_id
 WHERE NOT EXISTS (
    SELECT 1 FROM BORROW_ITEM AS bi
    JOIN BORROW_TRANSACTION AS t ON bi.transaction_id = t.transaction_id
    WHERE bi.item_id = e.item_id AND t.status IN ('RELEASED', 'CLOSED')
 );

-- Items per category
CREATE VIEW v_items_per_category AS
SELECT c.category_name, COUNT(e.item_id) AS total_items
 FROM CATEGORY AS c
 LEFT JOIN EQUIPMENT_ITEM AS e ON c.category_id = e.category_id
 GROUP BY c.category_id, c.category_name;

-- Users with multiple requests
CREATE VIEW v_users_multiple_requests AS
SELECT u.user_id, u.full_name, COUNT(t.transaction_id) AS total_requests
 FROM USER_ACCOUNT AS u
 JOIN BORROW_TRANSACTION AS t ON u.user_id = t.borrower_id
 GROUP BY u.user_id, u.full_name
 HAVING COUNT(t.transaction_id) > 1;

-- Unresolved maintenance issues
CREATE VIEW v_unresolved_maintenance AS
SELECT m.record_id, e.asset_tag, e.item_name, m.issue_description,
     m.status, m.reported_at, u.full_name AS reported_by
 FROM MAINTENANCE_RECORD AS m
 JOIN EQUIPMENT_ITEM AS e ON m.item_id = e.item_id
 JOIN USER_ACCOUNT AS u ON m.reported_by = u.user_id
 WHERE m.status IN ('OPEN', 'IN_PROGRESS');

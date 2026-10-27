-- Run after importing practice.sql.
-- These queries only read data. The report uses a fixed sample date.
USE gudo_im_practice;

-- 1. Check the tables and sample row counts.
SHOW TABLES;

SELECT 'USER_ACCOUNT' AS table_name, COUNT(*) AS row_count FROM USER_ACCOUNT
UNION ALL SELECT 'CATEGORY', COUNT(*) FROM CATEGORY
UNION ALL SELECT 'EQUIPMENT_ITEM', COUNT(*) FROM EQUIPMENT_ITEM
UNION ALL SELECT 'BORROW_TRANSACTION', COUNT(*) FROM BORROW_TRANSACTION
UNION ALL SELECT 'BORROW_ITEM', COUNT(*) FROM BORROW_ITEM
UNION ALL SELECT 'MAINTENANCE_RECORD', COUNT(*) FROM MAINTENANCE_RECORD;

-- 2. Show the two-part key and foreign keys.
SHOW CREATE TABLE BORROW_ITEM;

-- 3. Who still has equipment out?
SELECT t.transaction_id, u.full_name AS borrower, e.asset_tag, e.item_name,
       t.due_at,
       CASE WHEN t.due_at < '2026-10-28 12:00:00'
            THEN 'OVERDUE' ELSE 'ON_LOAN' END AS loan_state
FROM BORROW_TRANSACTION AS t
JOIN USER_ACCOUNT AS u ON u.user_id = t.borrower_id
JOIN BORROW_ITEM AS bi ON bi.transaction_id = t.transaction_id
JOIN EQUIPMENT_ITEM AS e ON e.item_id = bi.item_id
WHERE t.status = 'RELEASED' AND bi.returned_at IS NULL
ORDER BY t.due_at, t.transaction_id, e.asset_tag;

-- 4. Which units could be released, based on the current records?
SELECT e.asset_tag, e.item_name, c.category_name
FROM EQUIPMENT_ITEM AS e
JOIN CATEGORY AS c ON c.category_id = e.category_id
WHERE e.condition_status = 'GOOD'
  AND e.lifecycle_status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM BORROW_ITEM AS bi
      JOIN BORROW_TRANSACTION AS t ON t.transaction_id = bi.transaction_id
      WHERE bi.item_id = e.item_id
        AND t.status = 'RELEASED'
        AND bi.returned_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1
      FROM MAINTENANCE_RECORD AS m
      WHERE m.item_id = e.item_id AND m.status <> 'RESOLVED'
  )
ORDER BY e.asset_tag;

-- 5. Do any units appear in more than one active loan?
SELECT bi.item_id, COUNT(*) AS active_loan_count
FROM BORROW_ITEM AS bi
JOIN BORROW_TRANSACTION AS t ON t.transaction_id = bi.transaction_id
WHERE t.status = 'RELEASED' AND bi.returned_at IS NULL
GROUP BY bi.item_id
HAVING COUNT(*) > 1;

-- 6. Are any closed requests still missing a return?
SELECT t.transaction_id, bi.item_id
FROM BORROW_TRANSACTION AS t
JOIN BORROW_ITEM AS bi ON bi.transaction_id = t.transaction_id
WHERE t.status = 'CLOSED' AND bi.returned_at IS NULL;

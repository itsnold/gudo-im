-- Read-only report queries. Highlight and run one SELECT at a time.
USE campus_equipment_borrowing;

-- 1. Available equipment
SELECT * FROM v_available_equipment ORDER BY item_name;

-- 2. Currently borrowed equipment
SELECT * FROM v_currently_borrowed ORDER BY due_at, item_name;

-- 3. Overdue equipment
SELECT * FROM v_overdue_equipment ORDER BY due_at, borrower;

-- 4. Pending borrowing requests
SELECT * FROM v_pending_requests ORDER BY requested_at;

-- 5. Requests per user
SELECT * FROM v_requests_per_user ORDER BY total_requests DESC, full_name;

-- 6. Equipment never released
SELECT * FROM v_equipment_never_released ORDER BY item_name;

-- 7. Items per category
SELECT * FROM v_items_per_category ORDER BY total_items DESC, category_name;

-- 8. Users with multiple requests
SELECT * FROM v_users_multiple_requests ORDER BY total_requests DESC, full_name;

-- 9. Unresolved maintenance issues
SELECT * FROM v_unresolved_maintenance ORDER BY reported_at;

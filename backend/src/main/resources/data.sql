-- Default categories. ON CONFLICT keeps the script idempotent across
-- boots when schema.sql is set to mode=always.

INSERT INTO category (name, parent_id, sort_order) VALUES
    ('上装', NULL, 0),
    ('下装', NULL, 1),
    ('外套', NULL, 2),
    ('鞋',   NULL, 3),
    ('配饰', NULL, 4)
ON CONFLICT DO NOTHING;

-- category 表没有唯一约束，用 WHERE NOT EXISTS 避免重复插入
INSERT INTO category (name, parent_id, sort_order)
SELECT 'T恤', id, 0 FROM category WHERE name = '上装' AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM category WHERE name = 'T恤' AND parent_id IS NOT NULL)
UNION ALL SELECT '衬衫', id, 1 FROM category WHERE name = '上装' AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM category WHERE name = '衬衫' AND parent_id IS NOT NULL)
UNION ALL SELECT '毛衣', id, 2 FROM category WHERE name = '上装' AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM category WHERE name = '毛衣' AND parent_id IS NOT NULL)
UNION ALL SELECT '裤子', id, 0 FROM category WHERE name = '下装' AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM category WHERE name = '裤子' AND parent_id IS NOT NULL)
UNION ALL SELECT '裙子', id, 1 FROM category WHERE name = '下装' AND parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM category WHERE name = '裙子' AND parent_id IS NOT NULL);
INSERT INTO category (name, parent_id, sort_order) VALUES
    ('上装', NULL, 0),
    ('下装', NULL, 1),
    ('外套', NULL, 2),
    ('鞋',   NULL, 3),
    ('配饰', NULL, 4);

INSERT INTO category (name, parent_id, sort_order)
SELECT 'T恤', id, 0 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '衬衫', id, 1 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '毛衣', id, 2 FROM category WHERE name = '上装' AND parent_id IS NULL
UNION ALL SELECT '裤子', id, 0 FROM category WHERE name = '下装' AND parent_id IS NULL
UNION ALL SELECT '裙子', id, 1 FROM category WHERE name = '下装' AND parent_id IS NULL;

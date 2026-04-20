INSERT INTO `user` (
  `username`,
  `password_hash`,
  `role`,
  `status`,
  `nickname`,
  `phone`,
  `created_at`,
  `updated_at`,
  `created_by`,
  `updated_by`,
  `is_deleted`
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
  CONCAT('task9buyer', LPAD(n, 3, '0')),
  '$2a$10$uhDJzsgno7Aj4/ir7m2et.D7FmUWBVZqPAgHs3B0meqmkbciLDfRO',
  'USER',
  'ENABLED',
  CONCAT('Task9压测用户', LPAD(n, 3, '0')),
  CONCAT('1390000', LPAD(n, 4, '0')),
  NOW(3),
  NOW(3),
  NULL,
  NULL,
  0
FROM seq
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`),
  `nickname` = VALUES(`nickname`),
  `phone` = VALUES(`phone`),
  `updated_at` = NOW(3),
  `updated_by` = VALUES(`updated_by`),
  `is_deleted` = VALUES(`is_deleted`);

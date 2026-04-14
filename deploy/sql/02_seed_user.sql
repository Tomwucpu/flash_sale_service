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
VALUES
  ('admin', '$2a$10$uhDJzsgno7Aj4/ir7m2et.D7FmUWBVZqPAgHs3B0meqmkbciLDfRO', 'ADMIN', 'ENABLED', '平台管理员', '13800000000', NOW(3), NOW(3), NULL, NULL, 0),
  ('publisher', '$2a$10$uhDJzsgno7Aj4/ir7m2et.D7FmUWBVZqPAgHs3B0meqmkbciLDfRO', 'PUBLISHER', 'ENABLED', '活动发布方', '13800000002', NOW(3), NOW(3), NULL, NULL, 0),
  ('buyer', '$2a$10$uhDJzsgno7Aj4/ir7m2et.D7FmUWBVZqPAgHs3B0meqmkbciLDfRO', 'USER', 'ENABLED', '普通用户', '13800000003', NOW(3), NOW(3), NULL, NULL, 0)
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`),
  `nickname` = VALUES(`nickname`),
  `phone` = VALUES(`phone`),
  `updated_at` = NOW(3),
  `updated_by` = VALUES(`updated_by`),
  `is_deleted` = VALUES(`is_deleted`);

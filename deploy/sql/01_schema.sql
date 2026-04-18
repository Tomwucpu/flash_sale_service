CREATE DATABASE IF NOT EXISTS `flash_sale`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `flash_sale`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` VARCHAR(32) NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `nickname` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `activity_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT DEFAULT NULL,
  `cover_url` VARCHAR(255) DEFAULT NULL,
  `total_stock` INT NOT NULL,
  `available_stock` INT NOT NULL,
  `price_amount` DECIMAL(10, 2) NOT NULL,
  `need_payment` TINYINT NOT NULL,
  `purchase_limit_type` VARCHAR(32) NOT NULL,
  `purchase_limit_count` INT NOT NULL,
  `code_source_mode` VARCHAR(32) NOT NULL,
  `publish_mode` VARCHAR(32) NOT NULL,
  `publish_status` VARCHAR(32) NOT NULL,
  `publish_time` DATETIME(3) NOT NULL,
  `start_time` DATETIME(3) NOT NULL,
  `end_time` DATETIME(3) NOT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity_publish_time` (`publish_status`, `publish_time`),
  KEY `idx_activity_time_window` (`start_time`, `end_time`),
  KEY `idx_activity_creator` (`created_by`, `publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动商品表';

CREATE TABLE IF NOT EXISTS `redeem_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `code` VARCHAR(128) NOT NULL,
  `source_type` VARCHAR(32) NOT NULL,
  `batch_no` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `assigned_user_id` BIGINT DEFAULT NULL,
  `assigned_order_id` BIGINT DEFAULT NULL,
  `assigned_at` DATETIME(3) DEFAULT NULL,
  `invalid_reason` VARCHAR(128) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_redeem_code_code` (`code`),
  KEY `idx_redeem_code_activity_status` (`activity_id`, `status`),
  KEY `idx_redeem_code_batch` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兑换码表';

CREATE TABLE IF NOT EXISTS `redeem_code_import_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `total_count` INT NOT NULL,
  `success_count` INT NOT NULL,
  `failed_count` INT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_redeem_code_import_batch_no` (`batch_no`),
  KEY `idx_redeem_code_import_batch_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兑换码导入批次表';

CREATE TABLE IF NOT EXISTS `redeem_code_import_fail_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `batch_no` VARCHAR(64) NOT NULL,
  `line_no` INT NOT NULL,
  `raw_code` VARCHAR(128) DEFAULT NULL,
  `failure_reason` VARCHAR(64) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_redeem_code_import_fail_batch` (`batch_no`),
  KEY `idx_redeem_code_import_fail_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兑换码导入失败明细表';

CREATE TABLE IF NOT EXISTS `order_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `activity_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `request_id` VARCHAR(64) NOT NULL,
  `purchase_unique_key` VARCHAR(128) NOT NULL,
  `order_status` VARCHAR(32) NOT NULL,
  `pay_status` VARCHAR(32) NOT NULL,
  `code_status` VARCHAR(32) NOT NULL,
  `price_amount` DECIMAL(10, 2) NOT NULL,
  `fail_reason` VARCHAR(128) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_order_no` (`order_no`),
  UNIQUE KEY `uk_order_purchase_unique_key` (`purchase_unique_key`),
  KEY `idx_order_user_activity` (`user_id`, `activity_id`),
  KEY `idx_order_pay_code_status` (`pay_status`, `code_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `transaction_no` VARCHAR(64) NOT NULL,
  `pay_amount` DECIMAL(10, 2) NOT NULL,
  `pay_status` VARCHAR(32) NOT NULL,
  `callback_payload` JSON DEFAULT NULL,
  `paid_at` DATETIME(3) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_transaction_no` (`transaction_no`),
  KEY `idx_payment_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS `export_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `format` VARCHAR(16) NOT NULL,
  `filters_json` JSON DEFAULT NULL,
  `status` VARCHAR(32) NOT NULL,
  `file_url` VARCHAR(255) DEFAULT NULL,
  `fail_reason` VARCHAR(128) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `created_by` BIGINT DEFAULT NULL,
  `updated_by` BIGINT DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_export_activity_status` (`activity_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务表';

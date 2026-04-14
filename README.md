# 高并发秒杀兑换平台

本仓库当前已完成实施拆解文档中的 `Task 1: 基础工程与基础设施`，交付了多模块 Maven 骨架、公共模块、5 个业务服务启动工程、Docker Compose 本地基础设施和初始化 SQL。

## 技术基线

- Java 17
- Maven 3.9+
- Spring Boot 3.2.4
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.0
- MySQL 8.0
- Redis 7.x
- RabbitMQ 3.x
- Nacos 2.x

## 目录结构

```text
flash_sale_service/
├─ deploy/
│  ├─ docker-compose.yml
│  └─ sql/
│     └─ 01_schema.sql
├─ docs/
│  ├─ 高并发秒杀兑换平台功能需求与技术架构文档.md
│  ├─ 高并发秒杀兑换平台详细设计与实施拆解文档.md
│  ├─ 项目日志.md
│  └─ superpowers/plans/
│     └─ 2026-04-14-task1-m0-scaffold.md
├─ flash-sale-common/
│  ├─ common-core/
│  ├─ common-security/
│  ├─ common-redis/
│  ├─ common-mq/
│  └─ common-web/
├─ flash-sale-gateway/
├─ flash-sale-user-service/
├─ flash-sale-activity-service/
├─ flash-sale-seckill-service/
├─ flash-sale-order-service/
├─ flash-sale-payment-service/
├─ .env.example
└─ pom.xml
```

## 本地启动

1. 准备环境变量。

   ```powershell
   Copy-Item .env.example .env
   ```

2. 启动基础设施。

   ```powershell
   docker compose -f deploy/docker-compose.yml up -d
   ```

3. 初始化数据库。

   `deploy/sql/01_schema.sql` 已通过 MySQL 容器启动时自动加载。

4. 构建工程。

   ```powershell
   mvn -q -DskipTests package
   ```

## 端口约定

| 组件 | 端口 |
| --- | --- |
| Gateway | `8080` |
| User Service | `9001` |
| Activity Service | `9002` |
| Seckill Service | `9003` |
| Order Service | `9004` |
| Payment Service | `9005` |
| MySQL | `3306` |
| Redis | `6379` |
| RabbitMQ | `5672` |
| RabbitMQ Console | `15672` |
| Nacos | `8848` |

## 当前交付说明

- 已完成多模块 POM 结构与依赖管理。
- 已补齐共享响应体、用户上下文、Redis Key 和 MQ 事件模型。
- 已为 5 个业务服务创建启动类和统一配置占位。
- 已提供本地基础设施编排和核心库表初始化脚本。
- 下一步应进入 `Task 2: 认证与权限`。

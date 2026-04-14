# 高并发秒杀兑换平台

本仓库当前已完成实施拆解文档中的 `Task 1: 基础工程与基础设施` 与 `Task 2: 认证与权限`，交付了多模块 Maven 骨架、公共模块、5 个业务服务启动工程、本地基础设施、初始化 SQL，以及用户注册登录、JWT、网关鉴权和基础角色校验。

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
│     ├─ 01_schema.sql
│     └─ 02_seed_user.sql
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

3. 查看运行状态：
   ```powershell
   docker compose -f deploy/docker-compose.yml ps
   ```

4. 查看日志：
   ```powershell
   docker compose -f deploy/docker-compose.yml logs -f
   ```

5. 停止容器：
   ``` powershell
   docker compose -f deploy/docker-compose.yml down
   ```

6. 初始化数据库。

   `deploy/sql/01_schema.sql` 与 `deploy/sql/02_seed_user.sql` 会在 MySQL 数据目录首次初始化时自动加载。

   若本地已经存在 MySQL 或 Redis 数据卷，再修改 `.env` 中的账号、密码或端口配置时，需要同步更新已有容器状态，或重建旧卷；仅修改环境变量不会回写已初始化的数据目录。

7. 构建工程。

   ```powershell
   mvn -q -DskipTests package
   ```

## 默认联调账号

数据库首次初始化后会自动写入以下用户，默认密码统一为 `FlashSale@123`：

| 用户名 | 角色 | 用途 |
| --- | --- | --- |
| `admin` | `ADMIN` | 后台管理与角色校验联调 |
| `publisher` | `PUBLISHER` | 活动发布接口联调 |
| `buyer` | `USER` | 普通抢购用户联调 |

## 端口约定

| 组件 | 端口 |
| --- | --- |
| Gateway | `18080` |
| User Service | `9001` |
| Activity Service | `9002` |
| Seckill Service | `9003` |
| Order Service | `9004` |
| Payment Service | `9005` |
| MySQL | `3307` |
| Redis | `6379` |
| RabbitMQ | `5672` |
| RabbitMQ Console | `15672` |
| Nacos | `8848` |

`flash-sale-gateway` 默认读取 `APP_PORT`，本地默认值为 `18080`，用于避开部分 Windows 环境中 `8080` 所在的系统排除端口段；如本机 `8080` 可用，可自行覆盖。

## 当前交付说明

- 已完成多模块 POM 结构与依赖管理。
- 已补齐共享响应体、用户上下文、Redis Key 和 MQ 事件模型。
- 已为 5 个业务服务创建启动类和统一配置占位。
- 已提供本地基础设施编排和核心库表初始化脚本。
- 已完成用户注册、登录、当前用户查询和后台按角色查询用户的最小闭环。
- 已完成 JWT 生成解析、Gateway Bearer Token 鉴权和用户 Header 透传。
- 下一步应进入 `Task 3: 活动管理与发布`。

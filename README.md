# 高并发秒杀兑换平台

本仓库当前已完成实施拆解文档中的 `Task 1` 到 `Task 8` 主体功能，并开始沉淀 `Task 9: 联调与验收` 所需的订单查询、联调脚本、压测脚本与测试数据。当前代码已覆盖活动管理、公开活动浏览、秒杀入口、免费/支付型订单闭环、兑换码查询、模拟支付、导出审计与补偿台账。

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
│     ├─ 02_seed_user.sql
│     └─ 03_seed_task9_users.sql
├─ docs/
│  ├─ 高并发秒杀兑换平台功能需求与技术架构文档.md
│  ├─ 高并发秒杀兑换平台详细设计与实施拆解文档.md
│  ├─ 项目日志.md
│  ├─ task9-联调与验收说明.md
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
├─ scripts/
│  └─ task9/
│     ├─ data/payment-import-codes.csv
│     └─ task9_acceptance.py
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

   如需执行 Task 9 并发压测，可额外手动执行 `deploy/sql/03_seed_task9_users.sql`，初始化 `task9buyer001 ~ task9buyer050` 这批压测账号。

7. 构建工程。

   ```powershell
   mvn -q -DskipTests package
   ```

单独启动某个微服务时，服务会自动尝试导入模块目录和仓库根目录下的 `.env`，因此直接在对应模块下执行 `mvn spring-boot:run` 或从 IDE 启动即可复用本地联调配置。

当前 Gateway 路由使用 `lb://...` 转发到各微服务，因此本地联调时需要保持 `.env` 中 `NACOS_DISCOVERY_ENABLED=true`，并确保 `docker compose` 已启动 Nacos；否则微服务不会注册到服务发现，Gateway 会对 `/api/**` 返回 `503 Service Unavailable`。

## 默认联调账号

数据库首次初始化后会自动写入以下用户，默认密码统一为 `FlashSale@123`：

| 用户名 | 角色 | 用途 |
| --- | --- | --- |
| `admin` | `ADMIN` | 后台管理与角色校验联调 |
| `publisher` | `PUBLISHER` | 活动发布接口联调 |
| `buyer` | `USER` | 普通抢购用户联调 |

Task 9 压测用户通过 `deploy/sql/03_seed_task9_users.sql` 额外初始化，默认密码同样为 `FlashSale@123`。

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

## Task 9 自动化

- 冒烟联调：`python scripts/task9/task9_acceptance.py smoke`
- 本地压测：`python scripts/task9/task9_acceptance.py load-test --activity-id <活动ID> --users 20 --concurrency 20`
- 说明文档：`docs/task9-联调与验收说明.md`

脚本会把报告写到 `logs/task9/`，方便沉淀验收记录、压测结果和问题清单。

## 当前交付说明

- 已完成 `Task 1 ~ Task 8` 的主要后端能力，包括用户鉴权、活动发布、兑换码导入、秒杀入口、免费/支付型闭环、导出审计与补偿治理。
- 已打通 `flash-sale-payment-service` 的模拟支付与回调接口，以及 `flash-sale-order-service` 的订单详情、兑换码查询、导出任务与补偿台账接口。
- 已提供本地基础设施编排、核心库表初始化脚本和默认联调账号。
- 已为 Task 9 新增压测用户种子、第三方导入码样例文件，以及可复跑的联调/压测脚本。
- 当前仍待继续完成的主要方向是更完整的后台订单列表、真实第三方支付和更高可靠性的消息治理。

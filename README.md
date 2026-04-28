# 高并发秒杀兑换平台

基于 Spring Boot 3 + Spring Cloud Gateway + Vue 3 的前后端分离示例项目。当前仓库已经包含可运行的后端微服务、Vite 前端、默认联调数据，以及活动管理、公开活动浏览、秒杀、订单查询、模拟支付、兑换码导入/导出等完整链路。

## 项目概览

### 前端能力

- 公开端：平台首页、活动列表、活动详情、登录后参与抢购、自动轮询抢购结果、待支付订单模拟支付、活动内订单查询。
- 后台端：活动列表、新建/编辑/删除活动、立即发布/提前发布/下线、兑换码 `csv/xlsx` 导入、导入批次记录查看、活动订单查看、已售兑换码导出。
- 用户端：注册、登录、我的订单列表、订单状态查看、兑换码查看。

### 后端能力

- `flash-sale-gateway`：统一接入 `/api/**`，通过 Nacos 服务发现转发到各微服务。
- `flash-sale-user-service`：注册、登录、JWT 鉴权、当前用户信息查询。
- `flash-sale-activity-service`：活动 CRUD、公开活动查询、发布/下线、兑换码导入、Redis 活动缓存、定时发布扫描。
- `flash-sale-seckill-service`：基于 Redis + Lua 处理抢购请求，查询抢购结果，并通过 RabbitMQ 投递下单事件。
- `flash-sale-order-service`：消费下单事件、查询用户/发布方订单、生成兑换码、异步导出、补偿记录处理、超时未支付订单自动关闭。
- `flash-sale-payment-service`：创建模拟支付单、模拟支付回调、支付成功事件投递、支付超时消息投递。

### 技术栈

- 后端：Java 17、Spring Boot 3.2.4、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0、MyBatis-Plus、EasyExcel
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Vitest
- 基础设施：MySQL 8、Redis 7、RabbitMQ 3、Nacos 2

## 仓库结构

```text
flash_sale_service/
├─ deploy/
│  ├─ docker-compose.yml
│  └─ sql/
│     ├─ 01_schema.sql
│     └─ 02_seed_user.sql
├─ docs/
│  ├─ 前端接口文档.md
│  ├─ 后端接口文档.md
│  ├─ task9-联调与验收说明.md
│  ├─ 项目日志.md
│  ├─ 高并发秒杀兑换平台功能需求与技术架构文档.md
│  ├─ 高并发秒杀兑换平台详细设计与实施拆解文档.md
│  └─ redeem-code-import-template-100.xlsx
├─ flash-sale-common/
│  ├─ common-core/
│  ├─ common-mq/
│  ├─ common-redis/
│  ├─ common-security/
│  └─ common-web/
├─ flash-sale-gateway/
├─ flash-sale-user-service/
├─ flash-sale-activity-service/
├─ flash-sale-seckill-service/
├─ flash-sale-order-service/
├─ flash-sale-payment-service/
├─ frontend/
├─ logs/
├─ .env.example
├─ pom.xml
└─ README.md
```

## 快速开始

### 1. 环境准备

- JDK 17
- Maven 3.9+
- Node.js 与 npm
- Docker Desktop 或带 Compose 的 Docker 环境

### 2. 初始化配置

在仓库根目录复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

后端服务启动时会自动尝试加载模块目录和仓库根目录下的 `.env`。

### 3. 启动基础设施

```powershell
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

首次启动 MySQL 时会自动执行：

- `deploy/sql/01_schema.sql`
- `deploy/sql/02_seed_user.sql`

### 4. 安装后端模块依赖

首次本地联调前，先在根目录执行一次：

```powershell
mvn -q -DskipTests install
```

### 5. 启动后端服务

分别在 6 个终端中进入对应目录执行 `mvn spring-boot:run`：

| 模块 | 默认端口 | 说明 |
| --- | --- | --- |
| `flash-sale-user-service` | `9001` | 用户注册、登录、JWT |
| `flash-sale-activity-service` | `9002` | 活动管理、公开活动、兑换码导入 |
| `flash-sale-seckill-service` | `9003` | 抢购请求、结果查询 |
| `flash-sale-order-service` | `9004` | 订单、兑换码、导出、补偿 |
| `flash-sale-payment-service` | `9005` | 模拟支付与回调 |
| `flash-sale-gateway` | `18080` | 网关统一入口 |

示例：

```powershell
Set-Location .\flash-sale-user-service
mvn spring-boot:run
```

网关当前通过 `lb://...` 路由到各服务，本地联调时需要保持 `.env` 中 `NACOS_DISCOVERY_ENABLED=true`，并确保 Nacos 已启动；否则访问 `/api/**` 时网关会返回 `503 Service Unavailable`。

### 6. 启动前端

```powershell
Set-Location .\frontend
npm install
npm run dev
```

默认开发代理：

- `frontend/vite.config.ts` 将 `/api` 代理到 `http://localhost:18080`
- `frontend/src/api/http.ts` 支持通过 `VITE_API_BASE_URL` 覆盖 API 基址

### 7. 访问入口

- 前端首页：`http://localhost:5173/public/home`
- 前端登录页：`http://localhost:5173/login`
- 网关 API：`http://localhost:18080`
- RabbitMQ 管理台：`http://localhost:15672`
- Nacos 控制台：`http://localhost:8848/nacos`

## 默认账号

数据库初始化后会写入以下用户，默认密码统一为 `FlashSale@123`：

| 用户名 | 角色 | 用途 |
| --- | --- | --- |
| `admin` | `ADMIN` | 后台管理、活动与订单联调 |
| `publisher` | `PUBLISHER` | 发布方活动管理、导出兑换码 |
| `buyer` | `USER` | 公开活动抢购、模拟支付、我的订单 |

## 页面与路由

| 区域 | 路由 | 说明 |
| --- | --- | --- |
| 公开端 | `/public/home` | 平台首页 |
| 公开端 | `/public/activities` | 活动列表 |
| 公开端 | `/public/activities/:id` | 活动详情、抢购、模拟支付、活动内订单 |
| 通用 | `/login` | 登录页 |
| 通用 | `/register` | 注册页 |
| 后台 | `/admin/activities` | 活动管理列表 |
| 后台 | `/admin/activities/create` | 新建活动 |
| 后台 | `/admin/activities/:id` | 活动详情、导码批次、发布/下线 |
| 后台 | `/admin/activities/:id/edit` | 编辑活动 |
| 后台 | `/admin/activities/:id/orders` | 活动订单与兑换码导出 |
| 用户 | `/user/orders` | 我的订单 |

## 后端 API 概览

| 模块 | 路由前缀 | 说明 |
| --- | --- | --- |
| User Service | `/api/users` | 注册、登录、当前用户、用户查询 |
| Activity Service | `/api/activities` | 后台活动管理、发布、下线、导入兑换码 |
| Activity Service | `/api/public/activities` | 公开活动列表与详情 |
| Seckill Service | `/api/seckill` | 抢购发起、抢购结果查询 |
| Order Service | `/api/orders` | 我的订单、活动订单、发布方订单 |
| Order Service | `/api/exports` | 导出任务、补偿记录、文件下载 |
| Payment Service | `/api/payments` | 模拟支付单创建、模拟支付回调 |

更详细的接口说明见 `docs/后端接口文档.md`。

## 当前业务特性

- 活动支持免费和付费两种模式。
- 活动支持立即发布和定时发布；定时活动会由 `ActivityPublishScheduler` 定期扫描并发布。
- 秒杀请求由 Redis 库存键、限购键和 Lua 脚本共同控制，避免超卖。
- 抢购成功后会异步投递下单事件，前端活动详情页会自动轮询结果。
- 待支付订单默认按 15 分钟超时关闭，订单服务会定时扫描并执行关闭逻辑。
- 发布方可以查看活动订单，并异步导出已售兑换码文件。
- 导出文件默认写入服务运行目录下的 `exports/`，可通过 `FLASH_SALE_EXPORT_DIR` 覆盖。
- 各服务日志默认写入 `logs/<spring.application.name>.log`。

## 测试与构建

后端：

```powershell
mvn -q test
mvn -q -DskipTests package
```

前端：

```powershell
Set-Location .\frontend
npm test
npm run build
```

## 相关文档

- `docs/后端接口文档.md`：接口清单与请求/响应示例
- `docs/前端接口文档.md`：前端路由、页面调用链路与接口接入约定
- `docs/高并发秒杀兑换平台功能需求与技术架构文档.md`：需求与架构说明
- `docs/高并发秒杀兑换平台详细设计与实施拆解文档.md`：详细设计与任务拆解
- `docs/项目日志.md`：开发过程记录
- `docs/task9-联调与验收说明.md`：联调与验收历史说明

说明：`docs/` 下部分历史文档仍会提到早期的 `scripts/task9` 自动化脚本；当前仓库已经不包含该目录，联调与运行方式请以本 README 和现有源码为准。

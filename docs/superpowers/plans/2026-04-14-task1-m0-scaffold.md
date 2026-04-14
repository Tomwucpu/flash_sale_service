# Task 1 M0 Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建可持续扩展的秒杀兑换平台 M0 工程骨架，交付多模块 Maven 工程、基础公共模块、基础设施编排、统一配置约定和初始化库表脚本。  
**Architecture:** 以根聚合工程承载公共模块和 5 个业务服务，公共能力先收敛在 `flash-sale-common`，业务服务只做启动骨架和配置占位。基础设施通过 Docker Compose 在本地统一拉起，数据库通过初始化 SQL 直接建库建表。  
**Tech Stack:** Java 17、Maven 3.9、Spring Boot 3.2.4、Spring Cloud 2023.0.1、Spring Cloud Alibaba 2023.0.1.0、MySQL 8、Redis 7、RabbitMQ 3、Nacos 2。

---

### Task 1: 建立父工程与模块边界

**Files:**
- Create: `pom.xml`
- Create: `flash-sale-common/pom.xml`
- Create: `flash-sale-common/common-core/pom.xml`
- Create: `flash-sale-common/common-security/pom.xml`
- Create: `flash-sale-common/common-redis/pom.xml`
- Create: `flash-sale-common/common-mq/pom.xml`
- Create: `flash-sale-common/common-web/pom.xml`
- Create: `flash-sale-gateway/pom.xml`
- Create: `flash-sale-user-service/pom.xml`
- Create: `flash-sale-activity-service/pom.xml`
- Create: `flash-sale-seckill-service/pom.xml`
- Create: `flash-sale-order-service/pom.xml`
- Create: `flash-sale-payment-service/pom.xml`

- [ ] Step 1: 创建根聚合工程和公共聚合模块，锁定 Java 与 Spring 版本。
- [ ] Step 2: 为每个公共模块与业务服务建立独立 Maven 子模块。
- [ ] Step 3: 在业务服务中补齐启动所需 starter 依赖和 `spring-boot-maven-plugin`。

### Task 2: 先写共享模块的最小验证点

**Files:**
- Create: `flash-sale-common/common-core/src/test/java/com/flashsale/common/core/api/ApiResponseTest.java`
- Create: `flash-sale-common/common-security/src/test/java/com/flashsale/common/security/context/UserContextTest.java`
- Create: `flash-sale-common/common-redis/src/test/java/com/flashsale/common/redis/RedisKeysTest.java`
- Create: `flash-sale-common/common-mq/src/test/java/com/flashsale/common/mq/event/DomainEventTest.java`

- [ ] Step 1: 先写 `ApiResponse` 的成功/失败响应测试。
- [ ] Step 2: 先写用户上下文 Header 映射测试。
- [ ] Step 3: 先写 Redis Key 生成规则测试。
- [ ] Step 4: 先写统一事件模型测试并执行 `mvn -pl flash-sale-common/common-core,flash-sale-common/common-security,flash-sale-common/common-redis,flash-sale-common/common-mq test`，预期首次失败。

### Task 3: 实现公共基础代码

**Files:**
- Create: `flash-sale-common/common-core/src/main/java/com/flashsale/common/core/api/ApiResponse.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/context/UserContext.java`
- Create: `flash-sale-common/common-redis/src/main/java/com/flashsale/common/redis/RedisKeys.java`
- Create: `flash-sale-common/common-mq/src/main/java/com/flashsale/common/mq/event/DomainEvent.java`
- Create: `flash-sale-common/common-web/src/main/java/com/flashsale/common/web/GlobalExceptionHandler.java`

- [ ] Step 1: 用最小实现让响应体、用户上下文、Redis Key 和事件模型测试通过。
- [ ] Step 2: 增加统一异常处理占位，给后续服务直接复用。
- [ ] Step 3: 重新执行公共模块测试，预期全部通过。

### Task 4: 搭业务服务启动骨架与统一配置

**Files:**
- Create: `flash-sale-gateway/src/main/java/com/flashsale/gateway/FlashSaleGatewayApplication.java`
- Create: `flash-sale-gateway/src/main/resources/application.yml`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/FlashSaleUserApplication.java`
- Create: `flash-sale-user-service/src/main/resources/application.yml`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/FlashSaleActivityApplication.java`
- Create: `flash-sale-activity-service/src/main/resources/application.yml`
- Create: `flash-sale-seckill-service/src/main/java/com/flashsale/seckill/FlashSaleSeckillApplication.java`
- Create: `flash-sale-seckill-service/src/main/resources/application.yml`
- Create: `flash-sale-order-service/src/main/java/com/flashsale/order/FlashSaleOrderApplication.java`
- Create: `flash-sale-order-service/src/main/resources/application.yml`
- Create: `flash-sale-payment-service/src/main/java/com/flashsale/payment/FlashSalePaymentApplication.java`
- Create: `flash-sale-payment-service/src/main/resources/application.yml`

- [ ] Step 1: 为 5 个业务服务建立最小 Spring Boot 启动类。
- [ ] Step 2: 为每个服务补齐端口、日志、Nacos、MySQL、Redis、RabbitMQ 的配置占位。
- [ ] Step 3: 保持默认配置可本地构建，不把 Nacos 作为构建时强依赖。

### Task 5: 建立基础设施与文档资产

**Files:**
- Create: `deploy/docker-compose.yml`
- Create: `deploy/sql/01_schema.sql`
- Create: `.env.example`
- Create: `README.md`
- Create: `docs/项目日志.md`

- [ ] Step 1: 编写 MySQL、Redis、RabbitMQ、Nacos 的 Compose 编排。
- [ ] Step 2: 落库核心表结构和索引。
- [ ] Step 3: 补充环境变量示例、启动说明和 Task 1 项目日志。

### Task 6: 验证与收尾

**Files:**
- Verify: `pom.xml`
- Verify: `deploy/docker-compose.yml`

- [ ] Step 1: 运行公共模块测试，确认红绿循环完成。
- [ ] Step 2: 运行 `mvn -q -DskipTests package`，确认多模块结构可被 Maven 正常解析。
- [ ] Step 3: 运行 `docker compose -f deploy/docker-compose.yml config`，确认基础设施编排语法正确。

# Task 3 Activity Management And Publish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 打通活动 CRUD、立即发布、定时发布、下线和库存预热，为后续秒杀入口和兑换码导入提供可复用的活动管理基础。

**Architecture:** `flash-sale-activity-service` 负责活动持久化、发布校验、状态流转和调度扫描；活动发布时通过独立的 `ActivityCacheService` 预热 Redis 库存、详情和可见活动索引；后台接口统一复用 `common-security` 的角色校验能力，仅允许 `ADMIN` 和 `PUBLISHER` 管理活动。

**Tech Stack:** Spring Boot 3.2.4、MyBatis Plus、Spring Validation、Spring Scheduling、Spring Data Redis、H2、Spring Boot Test、MockMvc、Mockito。

---

### Task 1: 补齐 Task 3 模块依赖与测试基线

**Files:**
- Modify: `flash-sale-activity-service/pom.xml`
- Create: `flash-sale-activity-service/src/test/resources/application-test.yml`
- Create: `flash-sale-activity-service/src/test/resources/schema.sql`

- [ ] Step 1: 为活动服务增加 `spring-boot-starter-test` 与 `h2` 测试依赖。
- [ ] Step 2: 建立活动服务测试环境配置，关闭 Nacos，切到 H2 内存库。
- [ ] Step 3: 初始化 `activity_product` 与 `redeem_code` 测试表结构，覆盖 Task 3 所需字段。

### Task 2: 先写活动接口与状态流转测试

**Files:**
- Create: `flash-sale-activity-service/src/test/java/com/flashsale/activity/interfaces/ActivityAdminControllerTest.java`

- [ ] Step 1: 先写活动创建测试，验证默认 `UNPUBLISHED` 状态、库存回填和字段落库。
- [ ] Step 2: 先写活动编辑测试，验证草稿活动可修改。
- [ ] Step 3: 先写活动详情与列表测试，验证返回派生 `phase`。
- [ ] Step 4: 先写立即发布测试，验证发布状态更新并触发缓存预热。
- [ ] Step 5: 先写定时发布测试，验证接口仅登记待发布活动，不提前预热。
- [ ] Step 6: 先写下线测试，验证状态变更并清理缓存。
- [ ] Step 7: 先写第三方码量不足时禁止发布测试。

### Task 3: 先写调度与缓存组件测试

**Files:**
- Create: `flash-sale-activity-service/src/test/java/com/flashsale/activity/application/ActivityPublishSchedulerTest.java`
- Create: `flash-sale-activity-service/src/test/java/com/flashsale/activity/application/ActivityCacheServiceTest.java`

- [ ] Step 1: 先写调度扫描测试，验证到点的 `SCHEDULED` 活动会被发布并预热。
- [ ] Step 2: 先写缓存预热测试，验证写入库存 Key、活动详情 Key 和可见活动 ZSet。
- [ ] Step 3: 先写缓存清理测试，验证下线后删除库存/详情并移除可见活动索引。

### Task 4: 补最小实现并让测试转绿

**Files:**
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/ActivityEntity.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/ActivityPhase.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/CodeSourceMode.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/PublishMode.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/PublishStatus.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/domain/PurchaseLimitType.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/mapper/ActivityMapper.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/mapper/RedeemCodeMapper.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/service/ActivityService.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/service/ActivityCacheService.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/job/ActivityPublishScheduler.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/ActivityAdminController.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/dto/ActivityCreateRequest.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/dto/ActivityUpdateRequest.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/dto/ActivitySummaryResponse.java`
- Create: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/dto/ActivityDetailResponse.java`
- Modify: `flash-sale-activity-service/src/main/java/com/flashsale/activity/FlashSaleActivityApplication.java`
- Modify: `flash-sale-common/common-redis/src/main/java/com/flashsale/common/redis/RedisKeys.java`

- [ ] Step 1: 先补领域模型、枚举和 Mapper。
- [ ] Step 2: 实现活动创建、编辑、详情、列表服务逻辑和发布前校验。
- [ ] Step 3: 实现立即发布、定时发布、下线和到点发布逻辑。
- [ ] Step 4: 实现 Redis 预热与缓存清理组件。
- [ ] Step 5: 实现后台控制器，并接入 `@RequireRole({"ADMIN", "PUBLISHER"})`。
- [ ] Step 6: 开启调度能力，挂载定时扫描任务。

### Task 5: 更新文档与验证

**Files:**
- Modify: `docs/项目日志.md`
- Modify: `docs/后端接口文档.md`

- [ ] Step 1: 执行 `mvn -q -pl flash-sale-common/common-redis,flash-sale-activity-service -am test`。
- [ ] Step 2: 执行 `mvn -q -pl flash-sale-activity-service -am package -DskipTests`。
- [ ] Step 3: 更新项目日志，记录 Task 3 范围、测试命令和剩余风险。
- [ ] Step 4: 更新后端接口文档，补充活动管理相关接口。

# Activity Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后台活动管理补齐删除能力，允许未发布或已下线活动执行逻辑删除，并同步更新前端操作入口与测试。

**Architecture:** 继续沿用 `activity_product.is_deleted` 作为逻辑删除标记，在 `flash-sale-activity-service` 中新增删除接口和状态校验，不改动现有发布状态枚举；前端只在活动列表页增加删除按钮，通过现有 `activityApi` 与消息提示体系接入。后端删除时统一复用 `ActivityCacheService.clear` 做缓存清理，保证 Redis 库存、详情和活动可见索引无残留。

**Tech Stack:** Spring Boot 3.2.4、MyBatis Plus、MockMvc、Mockito、H2、Vue 3、TypeScript、Element Plus、Vitest。

---

### Task 1: 先补活动删除后端测试

**Files:**
- Modify: `flash-sale-activity-service/src/test/java/com/flashsale/activity/interfaces/ActivityAdminControllerTest.java`

- [ ] Step 1: 新增未发布活动删除测试，断言接口返回成功、数据库 `is_deleted = 1`、缓存清理被调用。
- [ ] Step 2: 新增已下线活动删除测试，断言接口返回成功且列表接口不再返回该活动。
- [ ] Step 3: 新增已发布活动删除拒绝测试，断言返回 `400 INVALID_ARGUMENT` 和准确错误文案。
- [ ] Step 4: 运行 `mvn -q -pl flash-sale-activity-service -Dtest=ActivityAdminControllerTest test`，确认新增测试先失败。

### Task 2: 补后端最小实现

**Files:**
- Modify: `flash-sale-activity-service/src/main/java/com/flashsale/activity/web/ActivityAdminController.java`
- Modify: `flash-sale-activity-service/src/main/java/com/flashsale/activity/service/ActivityService.java`

- [ ] Step 1: 在控制器中新增 `DELETE /api/activities/{activityId}` 接口，并复用现有角色校验与 `requestId` 透传。
- [ ] Step 2: 在服务中新增删除方法，校验只有 `UNPUBLISHED` 或 `OFFLINE` 活动允许删除。
- [ ] Step 3: 将活动标记为逻辑删除并记录 `updatedBy`，随后调用缓存清理。
- [ ] Step 4: 重新运行 `mvn -q -pl flash-sale-activity-service -Dtest=ActivityAdminControllerTest test`，确认测试转绿。

### Task 3: 接入前端删除动作

**Files:**
- Modify: `frontend/src/api/activity.ts`
- Modify: `frontend/src/views/admin/ActivityListView.vue`

- [ ] Step 1: 在活动 API 中新增删除方法，对接 `DELETE /api/activities/{activityId}`。
- [ ] Step 2: 在列表页新增删除处理函数、确认弹窗和成功提示。
- [ ] Step 3: 在操作列中增加删除按钮，已发布活动禁用，未发布和已下线活动可删除。
- [ ] Step 4: 更新操作提示文案，让页面提示与后端规则一致。

### Task 4: 更新文档并验证

**Files:**
- Modify: `docs/后端接口文档.md`
- Modify: `docs/项目日志.md`

- [ ] Step 1: 更新后端接口文档，补充删除接口和删除规则。
- [ ] Step 2: 更新项目日志，记录本次删除能力范围、限制和验证命令。
- [ ] Step 3: 运行 `mvn -q -pl flash-sale-activity-service -am test`。
- [ ] Step 4: 运行 `npm test`。
- [ ] Step 5: 运行 `npm run build`。

# Task 9 联调与验收说明

## 目标

把 Task 9 需要的三类东西沉淀进仓库，方便反复联调而不是靠手工记忆：

- 联调测试数据准备
- 免费活动 / 支付活动完整冒烟脚本
- 本地并发压测脚本与结果落盘

## 新增交付物

- `deploy/sql/03_seed_task9_users.sql`
  - 生成 `task9buyer001 ~ task9buyer050` 共 50 个压测用户
  - 默认密码统一为 `FlashSale@123`
- `scripts/task9/data/payment-import-codes.csv`
  - 支付型第三方导入码活动的默认联调码池
- `scripts/task9/task9_acceptance.py`
  - `smoke` 子命令：自动创建活动、导码、发布、秒杀、支付、导出并生成报告
  - `load-test` 子命令：并发打秒杀接口并输出 QPS、延迟和最终状态统计

## 使用前提

1. 启动基础设施与所有微服务，确保 Gateway 可从 `http://localhost:18080` 转发。
2. 确保默认账号 `publisher`、`buyer` 已初始化。
3. 如需压测，执行以下 SQL 初始化批量用户：

```sql
source deploy/sql/03_seed_task9_users.sql;
```

## 冒烟联调

命令：

```powershell
python scripts/task9/task9_acceptance.py smoke
```

脚本会自动完成：

1. 使用 `publisher` 登录并创建两个活动：
   - 免费 + 系统生成码
   - 支付 + 第三方导入码
2. 为支付活动导入 `payment-import-codes.csv`
3. 发布两个活动
4. 使用 `buyer` 走完整免费链路：
   - 秒杀
   - 轮询结果
   - 按活动查询订单与兑换码列表
5. 使用 `buyer` 走完整支付链路：
   - 秒杀
   - 轮询待支付
   - 创建支付单
   - 支付回调
   - 轮询成功结果
   - 按活动查询订单与兑换码列表
6. 使用 `publisher` 发起导出并下载导出文件

输出位置：

- `logs/task9/smoke-时间戳/task9-smoke-report.json`
- `logs/task9/smoke-时间戳/task9-smoke-report.md`
- 如导出成功，还会把下载文件一起落到同目录

## 本地并发压测

先用 `smoke` 生成一个可用活动，记录免费活动的 `activityId`，再执行：

```powershell
python scripts/task9/task9_acceptance.py load-test --activity-id 123 --users 20 --concurrency 20
```

说明：

- 压测用户默认使用 `task9buyer001` 开始的批量账号
- 每个用户只发起一次秒杀，适合验证：
  - 不超卖
  - 一人一买
  - 不重复建单
  - 最终状态分布
- 结果会输出到：

```text
logs/task9/load-时间戳/task9-load-test-report.json
```

重点关注字段：

- `qps`
- `latencyMs.p50 / p95 / p99`
- `attemptCodes`
- `finalStatuses`

## 建议验收口径

免费链路至少确认：

- `result.status = SUCCESS`
- `order.payStatus = NO_NEED`
- `order.codeStatus = ISSUED`
- 订单记录中的 `code` 字段可查到兑换码

支付链路至少确认：

- 首次结果为 `PENDING_PAYMENT`
- 回调后结果转为 `SUCCESS`
- `order.payStatus = PAID`
- 订单记录中的 `code` 字段最终可查

导出链路至少确认：

- 导出任务状态转为 `SUCCESS`
- `fileUrl` 可下载
- 文件内容包含目标订单

并发链路至少确认：

- `SUCCESS + FAIL + PENDING_PAYMENT` 的最终数量与请求数闭合
- 不出现负库存
- 不出现同一用户多单成功
- 不出现重复发码

# Task 6 Free Order Code Closure Design

## Context

Current project status after Task 5:

- `flash-sale-seckill-service` already completes Redis pre-deduction, purchase-limit control, request idempotency, `order.create` event publishing, and `seckill:result` initial `PROCESSING` writeback.
- `flash-sale-order-service` is still an empty shell and does not yet consume `order.create`, persist orders, assign codes, write final seckill results, or compensate Redis state on failure.
- The current live `order.create` payload contains `activityId`, `userId`, `requestId`, `needPayment`, and `codeSourceMode`.

This design covers the next real delivery slice: free-activity order creation and code issuance closure, plus a user-facing code query endpoint for completed orders.

## Goal

Implement Task 6 as a real end-to-end free-flow closure:

- consume `order.create` for free activities;
- create the order record;
- issue a code from either imported-code or system-generated mode;
- write the final `seckill:result` status back to Redis;
- compensate Redis stock and purchase-limit counters when the flow fails before success is finalized;
- expose `GET /api/codes/orders/{orderNo}` so the owning user can query the issued code.

## In Scope

- `flash-sale-order-service` order consumer for `order.create`
- free-activity order persistence in `order_record`
- imported code assignment from `redeem_code`
- system-generated code creation in `redeem_code`
- Redis result writeback to `seckill:result:{activityId}:{userId}`
- Redis compensation for stock and purchase-limit counters on final free-flow failure
- user-facing code query API by `orderNo`
- tests, docs, and project log updates related to Task 6

## Out of Scope

- payment flow and `WAIT_PAY` lifecycle
- `payment.success` consumption
- timeout close flow
- export tasks
- dead-letter governance and manual compensation console
- introducing a new `limitSlot` field into the current `order.create` event

## Decision Summary

### 1. Service boundary

Implement Task 6 directly inside `flash-sale-order-service`, operating on the shared tables already defined in the repo:

- `activity_product`
- `order_record`
- `redeem_code`

Reasoning:

- this matches the current repository shape and the current stage of the project;
- it minimizes cross-service plumbing for the first complete closure;
- it keeps the free-flow implementation aligned with the existing single-database local integration model.

### 2. Current idempotency key

Use the current real event contract rather than expanding it in Task 6.

For this iteration:

- request idempotency remains `activityId + userId + requestId` in the seckill layer;
- order consumer idempotency is based on `requestId` and a derived `purchaseUniqueKey`;
- `purchaseUniqueKey` will be stored as `activity:{activityId}:user:{userId}:req:{requestId}`.

This does not perfectly match the document's future-facing `limitSlot` wording, but it matches the real upstream payload and is sufficient for the current free-flow closure. If Task 7 or Task 9 later needs a stronger multi-purchase model, the event can be extended in a focused follow-up.

### 3. Free-flow failure policy

For Task 6, these failures trigger Redis compensation:

- order creation fails before success is finalized;
- imported code is unavailable or cannot be assigned;
- an unexpected exception occurs before the order reaches final success.

System-generated code failure also results in a failed order and `FAIL` result for this iteration, with Redis compensation applied because the free-flow closure did not complete successfully.

## Functional Design

## 1. MQ consumer

Add an `order.create` consumer in `flash-sale-order-service`.

Responsibilities:

- deserialize `DomainEvent`
- ignore or short-circuit duplicate messages safely
- route only free-activity messages into the Task 6 closure path
- leave payment messages on a clean branch for Task 7 follow-up

Expected queue/exchange alignment:

- exchange: `flash.sale.event.exchange`
- routing key: `order.create`
- queue: `flash.sale.order.create.queue`

Consumer rule:

- only ACK after business handling completes successfully;
- duplicate deliveries must not create duplicate orders or duplicate code issuance.

## 2. Order creation flow

For `needPayment=false` messages:

1. Load the referenced activity row from `activity_product`.
2. Reject deleted or missing activities as final failure.
3. Build a deterministic `purchaseUniqueKey` from activity, user, and request id.
4. Check whether an order already exists for the same business key.
5. If a final successful order already exists, treat the message as idempotent success and rewrite Redis result if needed.
6. If no order exists, insert a new order with:
   - `orderStatus=INIT`
   - `payStatus=NO_NEED`
   - `codeStatus=PENDING`
   - `priceAmount=0.00`
   - `requestId` from the event
7. Issue a code according to `codeSourceMode`.
8. On success, update the order to:
   - `orderStatus=CONFIRMED`
   - `codeStatus=ISSUED`
9. Write the final seckill result as `SUCCESS`.

## 3. Imported-code assignment

For `codeSourceMode=THIRD_PARTY_IMPORTED`:

- select one `redeem_code` row under the same activity where:
  - `status=AVAILABLE`
  - `is_deleted=0`
- claim it atomically by updating:
  - `status=ASSIGNED`
  - `assigned_user_id`
  - `assigned_order_id`
  - `assigned_at`
- if no row can be claimed, finalize the flow as failure

Expected final failure semantics:

- order becomes `FAILED`
- `fail_reason` is populated with a code-pool shortage style reason
- Redis stock and limit are compensated
- `seckill:result` becomes `FAIL`

## 4. System-generated code issuance

For `codeSourceMode=SYSTEM_GENERATED`:

- generate a unique platform code during issuance
- insert a new `redeem_code` row with:
  - matching `activity_id`
  - generated `code`
  - `source_type=SYSTEM_GENERATED`
  - `status=ASSIGNED`
  - assigned user/order fields filled
- if uniqueness conflicts occur, retry generation for a bounded number of attempts
- if generation or insert still fails, mark the order as `FAILED`

The Task 6 rule is explicit:

- no fallback to imported-code mode
- no silent success without a persisted code record

## 5. Redis seckill result writeback

Write back to `seckill:result:{activityId}:{userId}` with the existing hash structure:

- `status`
- `orderNo`
- `message`
- `code`
- `updatedAt`

Final states used in Task 6:

- success:
  - `status=SUCCESS`
  - `message=抢购成功`
  - `orderNo=<final order no>`
  - `code=<issued code>`
- failure:
  - `status=FAIL`
  - `message=<failure reason>`
  - `orderNo=<failed order no if created, otherwise empty>`
  - `code=` empty

The key TTL should remain aligned with the existing seckill result lifecycle, meaning the writeback should preserve or reset expiry to the activity-end-plus-buffer pattern already used by the seckill flow.

## 6. Redis compensation

When the free-flow fails before final success:

- increment `seckill:stock:{activityId}` by 1
- decrement `seckill:limit:{activityId}:{userId}` by 1, but never leave a negative effective value
- write `seckill:result` as `FAIL`

Compensation must be idempotent relative to repeated message deliveries. The implementation should avoid double compensation when the message is replayed after a prior final failure has already been persisted.

## 7. Code query API

Add:

- `GET /api/codes/orders/{orderNo}`

Behavior:

- authenticated user only
- only the owning user may query the order's code
- return the associated order and code issuance state
- if the order does not exist, return not found style business failure
- if the order belongs to another user, return forbidden

Recommended response fields:

- `orderNo`
- `activityId`
- `orderStatus`
- `payStatus`
- `codeStatus`
- `code`
- `updatedAt`

For a free-flow order that completed successfully, `code` must be directly returned.

## Data Design

## 1. New order-service entities

Add order-service-local entities that map the existing shared tables:

- `ActivityProductEntity`
- `OrderRecordEntity`
- `RedeemCodeEntity`

Reason:

- `flash-sale-order-service` should not depend on activity-service implementation classes directly;
- duplicate table mapping is acceptable here because the shared database contract is already explicit in this repo.

## 2. Order number generation

Generate `orderNo` inside order-service using a deterministic, readable strategy suitable for demos and tests.

Requirement:

- unique across inserts
- easy to assert in tests

A timestamp-plus-sequence style string is acceptable for this iteration.

## 3. Fail reason vocabulary

Standardize a small set of failure reasons for Task 6, for example:

- `ACTIVITY_NOT_FOUND`
- `IMPORTED_CODE_UNAVAILABLE`
- `SYSTEM_CODE_GENERATION_FAILED`
- `ORDER_CREATE_FAILED`

The exact stored text can remain concise, but tests and Redis writeback should assert stable outward-facing semantics rather than incidental exception messages.

## Testing Strategy

Follow strict TDD.

## 1. Consumer tests

Create consumer/application tests first for:

- imported-code free order succeeds and writes final Redis success result
- system-generated free order succeeds and writes final Redis success result
- duplicate `order.create` delivery does not create a second order or second code assignment
- imported-code shortage produces failed order/result and compensation
- unexpected persistence failure produces compensation and failure result

## 2. API tests

Create controller tests for:

- owner can query issued code by `orderNo`
- unknown order returns not found style error
- non-owner query is rejected

## 3. Persistence assertions

Tests should assert real database rows for:

- `order_record`
- `redeem_code`

Tests should also assert Redis interactions:

- success writeback
- failure writeback
- stock compensation
- limit compensation

## Implementation Steps

1. Add order-service test dependencies and test profile resources if missing.
2. Write failing tests for imported-code success flow.
3. Implement minimal entities, mappers, consumer, and writeback logic to pass.
4. Write failing tests for system-generated success flow.
5. Implement bounded unique code generation and persistence.
6. Write failing tests for duplicate-delivery idempotency.
7. Implement final-state deduplication behavior.
8. Write failing tests for compensation paths.
9. Implement Redis compensation and failure result writeback.
10. Write failing tests for `GET /api/codes/orders/{orderNo}`.
11. Implement query API and ownership check.
12. Run module tests, then broader verification.
13. Update `docs/后端接口文档.md` and `docs/项目日志.md`.

## Risks and Follow-ups

- The current upstream event does not carry a `limitSlot`, so multi-purchase uniqueness is intentionally deferred.
- Shared-table modeling across services is pragmatic for this repo stage, but later service extraction would require contract cleanup.
- Redis compensation is only as reliable as current message finalization behavior; stronger producer confirmation or local-message-table patterns remain a later enhancement.
- Payment flow remains intentionally untouched and should be handled in Task 7 rather than mixed into this iteration.

## Acceptance Criteria

Task 6 is considered complete for this iteration when all of the following are true:

- a free seckill attempt can reach final `SUCCESS` through imported-code mode
- a free seckill attempt can reach final `SUCCESS` through system-generated mode
- final success writes `orderNo` and `code` into `seckill:result`
- imported-code shortage or equivalent final failure writes `FAIL` and compensates Redis stock and purchase-limit counters
- duplicate `order.create` delivery is idempotent
- `GET /api/codes/orders/{orderNo}` returns the issued code for the owning user
- docs and project log are updated to reflect the delivered Task 6 behavior

#!/usr/bin/env python
from __future__ import annotations

import argparse
import json
import math
import os
import statistics
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any
from urllib import error, parse, request


DEFAULT_BASE_URL = "http://localhost:18080"
DEFAULT_PASSWORD = "FlashSale@123"
DEFAULT_PUBLISHER = "publisher"
DEFAULT_BUYER = "buyer"
DEFAULT_LOAD_USER_PREFIX = "task9buyer"
ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_CODE_FILE = Path(__file__).resolve().parent / "data" / "payment-import-codes.csv"


def now_text() -> str:
    return datetime.now().strftime("%Y%m%d-%H%M%S")


def iso_seconds(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def ensure_dir(path: Path) -> Path:
    path.mkdir(parents=True, exist_ok=True)
    return path


@dataclass
class HttpResult:
    status_code: int
    body: dict[str, Any]
    elapsed_ms: float


class FlashSaleClient:
    def __init__(self, base_url: str, timeout: float = 15.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request_json(
        self,
        method: str,
        path: str,
        payload: dict[str, Any] | None = None,
        token: str | None = None,
        request_id: str | None = None,
    ) -> HttpResult:
        headers = {
            "Accept": "application/json",
        }
        if payload is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(payload).encode("utf-8")
        else:
            data = None
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if request_id:
            headers["X-Request-Id"] = request_id
        req = request.Request(
            self.base_url + path,
            data=data,
            headers=headers,
            method=method.upper(),
        )
        started = time.perf_counter()
        try:
            with request.urlopen(req, timeout=self.timeout) as response:
                raw = response.read().decode("utf-8")
                body = json.loads(raw) if raw else {}
                return HttpResult(response.status, body, (time.perf_counter() - started) * 1000)
        except error.HTTPError as exc:
            raw = exc.read().decode("utf-8")
            body = json.loads(raw) if raw else {}
            return HttpResult(exc.code, body, (time.perf_counter() - started) * 1000)

    def request_multipart(
        self,
        path: str,
        file_path: Path,
        field_name: str,
        token: str,
        request_id: str,
    ) -> HttpResult:
        boundary = f"----FlashSaleTask9{uuid.uuid4().hex}"
        headers = {
            "Accept": "application/json",
            "Authorization": f"Bearer {token}",
            "X-Request-Id": request_id,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        }
        file_bytes = file_path.read_bytes()
        disposition = (
            f'--{boundary}\r\n'
            f'Content-Disposition: form-data; name="{field_name}"; filename="{file_path.name}"\r\n'
            "Content-Type: text/csv\r\n\r\n"
        ).encode("utf-8")
        ending = f"\r\n--{boundary}--\r\n".encode("utf-8")
        data = disposition + file_bytes + ending
        req = request.Request(
            self.base_url + path,
            data=data,
            headers=headers,
            method="POST",
        )
        started = time.perf_counter()
        try:
            with request.urlopen(req, timeout=self.timeout) as response:
                raw = response.read().decode("utf-8")
                body = json.loads(raw) if raw else {}
                return HttpResult(response.status, body, (time.perf_counter() - started) * 1000)
        except error.HTTPError as exc:
            raw = exc.read().decode("utf-8")
            body = json.loads(raw) if raw else {}
            return HttpResult(exc.code, body, (time.perf_counter() - started) * 1000)

    def download(self, path: str, token: str, target_path: Path) -> Path:
        req = request.Request(
            self.base_url + path,
            headers={"Authorization": f"Bearer {token}", "Accept": "*/*"},
            method="GET",
        )
        with request.urlopen(req, timeout=self.timeout) as response:
            target_path.write_bytes(response.read())
        return target_path


def assert_success(result: HttpResult, message: str) -> dict[str, Any]:
    if result.status_code != 200 or result.body.get("code") not in {"SUCCESS", "SECKILL_PROCESSING"}:
        raise RuntimeError(f"{message} failed: http={result.status_code}, body={json.dumps(result.body, ensure_ascii=False)}")
    return result.body


def login(client: FlashSaleClient, username: str, password: str) -> str:
    result = client.request_json(
        "POST",
        "/api/users/login",
        {"username": username, "password": password},
        request_id=f"LOGIN-{username}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"login {username}")
    return body["data"]["accessToken"]


def create_activity(
    client: FlashSaleClient,
    token: str,
    title: str,
    need_payment: bool,
    price_amount: str,
    total_stock: int,
    code_source_mode: str,
) -> dict[str, Any]:
    now = datetime.now()
    payload = {
        "title": title,
        "description": f"{title} - Task 9 acceptance flow",
        "coverUrl": "https://example.com/task9-cover.png",
        "totalStock": total_stock,
        "priceAmount": price_amount,
        "needPayment": need_payment,
        "purchaseLimitType": "SINGLE",
        "purchaseLimitCount": 1,
        "codeSourceMode": code_source_mode,
        "publishMode": "IMMEDIATE",
        "publishTime": iso_seconds(now),
        "startTime": iso_seconds(now - timedelta(minutes=1)),
        "endTime": iso_seconds(now + timedelta(hours=2)),
    }
    result = client.request_json(
        "POST",
        "/api/activities",
        payload,
        token=token,
        request_id=f"ACTIVITY-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"create activity {title}")
    return body["data"]


def publish_activity(client: FlashSaleClient, token: str, activity_id: int) -> dict[str, Any]:
    result = client.request_json(
        "POST",
        f"/api/activities/{activity_id}/publish",
        token=token,
        request_id=f"PUBLISH-{activity_id}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"publish activity {activity_id}")
    return body["data"]


def import_codes(client: FlashSaleClient, token: str, activity_id: int, file_path: Path) -> dict[str, Any]:
    result = client.request_multipart(
        f"/api/activities/{activity_id}/codes/import",
        file_path,
        "file",
        token,
        f"IMPORT-{activity_id}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"import codes for {activity_id}")
    return body["data"]


def attempt_seckill(client: FlashSaleClient, token: str, activity_id: int, request_id: str) -> dict[str, Any]:
    result = client.request_json(
        "POST",
        f"/api/seckill/activities/{activity_id}/attempt",
        token=token,
        request_id=request_id,
    )
    if result.status_code != 200:
        raise RuntimeError(f"attempt activity {activity_id} failed: http={result.status_code}, body={json.dumps(result.body, ensure_ascii=False)}")
    return result.body


def poll_result(
    client: FlashSaleClient,
    token: str,
    activity_id: int,
    expected_statuses: set[str],
    timeout_seconds: float = 12.0,
) -> dict[str, Any]:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        result = client.request_json(
            "GET",
            f"/api/seckill/results/{activity_id}",
            token=token,
            request_id=f"RESULT-{activity_id}-{uuid.uuid4().hex[:8]}",
        )
        body = assert_success(result, f"query seckill result {activity_id}")
        data = body["data"]
        if data.get("status") in expected_statuses:
            return data
        time.sleep(0.5)
    raise RuntimeError(f"poll result timeout for activity {activity_id}")


def query_order(client: FlashSaleClient, token: str, order_no: str) -> dict[str, Any]:
    result = client.request_json(
        "GET",
        f"/api/orders/{parse.quote(order_no)}",
        token=token,
        request_id=f"ORDER-{order_no}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"query order {order_no}")
    return body["data"]


def query_code(client: FlashSaleClient, token: str, order_no: str) -> dict[str, Any]:
    result = client.request_json(
        "GET",
        f"/api/codes/orders/{parse.quote(order_no)}",
        token=token,
        request_id=f"CODE-{order_no}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"query code {order_no}")
    return body["data"]


def create_payment(client: FlashSaleClient, token: str, order_no: str) -> dict[str, Any]:
    result = client.request_json(
        "POST",
        f"/api/payments/orders/{parse.quote(order_no)}",
        token=token,
        request_id=f"PAY-{order_no}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"create payment {order_no}")
    return body["data"]


def callback_payment(client: FlashSaleClient, token: str, order_no: str, transaction_no: str) -> dict[str, Any]:
    result = client.request_json(
        "POST",
        "/api/payments/callback",
        {"orderNo": order_no, "transactionNo": transaction_no},
        token=token,
        request_id=f"CALLBACK-{order_no}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"callback payment {order_no}")
    return body["data"]


def create_export_task(client: FlashSaleClient, token: str, activity_id: int) -> dict[str, Any]:
    payload = {
        "activityId": activity_id,
        "format": "CSV",
        "filters": {
            "codeStatus": "ISSUED"
        },
    }
    result = client.request_json(
        "POST",
        "/api/exports/tasks",
        payload,
        token=token,
        request_id=f"EXPORT-{activity_id}-{uuid.uuid4().hex[:8]}",
    )
    body = assert_success(result, f"create export task {activity_id}")
    return body["data"]


def poll_export_task(
    client: FlashSaleClient,
    token: str,
    task_id: int,
    timeout_seconds: float = 15.0,
) -> dict[str, Any]:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        result = client.request_json(
            "GET",
            f"/api/exports/tasks/{task_id}",
            token=token,
            request_id=f"EXPORT-TASK-{task_id}-{uuid.uuid4().hex[:8]}",
        )
        body = assert_success(result, f"query export task {task_id}")
        data = body["data"]
        if data.get("status") in {"SUCCESS", "FAILED"}:
            return data
        time.sleep(0.5)
    raise RuntimeError(f"poll export task timeout for task {task_id}")


def percentile(values: list[float], percent: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    rank = max(0, math.ceil(percent * len(ordered)) - 1)
    return round(ordered[rank], 2)


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_smoke_markdown(path: Path, report: dict[str, Any]) -> None:
    free_flow = report["freeFlow"]
    paid_flow = report["paidFlow"]
    export_flow = report["exportFlow"]
    issues = report.get("issues", [])
    lines = [
        "# Task 9 联调验收记录",
        "",
        f"- 生成时间：`{report['generatedAt']}`",
        f"- Base URL：`{report['baseUrl']}`",
        "",
        "## 活动准备",
        "",
        f"- 免费活动：`{report['activities']['free']['id']}` `{report['activities']['free']['title']}`",
        f"- 支付活动：`{report['activities']['paid']['id']}` `{report['activities']['paid']['title']}`",
        f"- 支付活动导入码成功数：`{report['activities']['paidImport']['successCount']}`",
        "",
        "## 免费链路",
        "",
        f"- 秒杀结果：`{free_flow['result']['status']}`",
        f"- 订单号：`{free_flow['result']['orderNo']}`",
        f"- 订单状态：`{free_flow['order']['orderStatus']}` / `payStatus={free_flow['order']['payStatus']}` / `codeStatus={free_flow['order']['codeStatus']}`",
        f"- 兑换码：`{free_flow['code']['code']}`",
        "",
        "## 支付链路",
        "",
        f"- 秒杀结果：`{paid_flow['pendingResult']['status']}`",
        f"- 订单号：`{paid_flow['pendingResult']['orderNo']}`",
        f"- 支付单：`{paid_flow['payment']['transactionNo']}`",
        f"- 回调后状态：`{paid_flow['successResult']['status']}`",
        f"- 订单状态：`{paid_flow['order']['orderStatus']}` / `payStatus={paid_flow['order']['payStatus']}` / `codeStatus={paid_flow['order']['codeStatus']}`",
        f"- 兑换码：`{paid_flow['code']['code']}`",
        "",
        "## 导出链路",
        "",
        f"- 导出任务：`{export_flow['task']['id']}`",
        f"- 导出状态：`{export_flow['taskStatus']['status']}`",
        f"- 下载地址：`{export_flow['taskStatus'].get('fileUrl', '')}`",
        f"- 本地文件：`{export_flow.get('downloadedFile', '')}`",
        "",
        "## 问题清单",
        "",
    ]
    if issues:
        for issue in issues:
            lines.append(f"- {issue}")
    else:
        lines.append("- 本轮脚本联调未记录新增问题。")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def run_smoke(args: argparse.Namespace) -> None:
    client = FlashSaleClient(args.base_url, timeout=args.timeout)
    output_dir = ensure_dir(ROOT_DIR / "logs" / "task9" / f"smoke-{now_text()}")

    publisher_token = login(client, args.publisher_user, args.password)
    buyer_token = login(client, args.buyer_user, args.password)

    free_activity = create_activity(
        client,
        publisher_token,
        title=f"Task9免费活动-{now_text()}",
        need_payment=False,
        price_amount="0.00",
        total_stock=5,
        code_source_mode="SYSTEM_GENERATED",
    )
    paid_activity = create_activity(
        client,
        publisher_token,
        title=f"Task9支付活动-{now_text()}",
        need_payment=True,
        price_amount="19.90",
        total_stock=5,
        code_source_mode="THIRD_PARTY_IMPORTED",
    )
    import_result = import_codes(client, publisher_token, paid_activity["id"], Path(args.code_file))
    publish_activity(client, publisher_token, free_activity["id"])
    publish_activity(client, publisher_token, paid_activity["id"])

    free_attempt = attempt_seckill(client, buyer_token, free_activity["id"], f"FREE-{uuid.uuid4().hex[:8]}")
    free_result = poll_result(client, buyer_token, free_activity["id"], {"SUCCESS"})
    free_order = query_order(client, buyer_token, free_result["orderNo"])
    free_code = query_code(client, buyer_token, free_result["orderNo"])

    paid_attempt = attempt_seckill(client, buyer_token, paid_activity["id"], f"PAID-{uuid.uuid4().hex[:8]}")
    paid_pending = poll_result(client, buyer_token, paid_activity["id"], {"PENDING_PAYMENT"})
    create_payment_result = create_payment(client, buyer_token, paid_pending["orderNo"])
    callback_payment(client, buyer_token, paid_pending["orderNo"], create_payment_result["transactionNo"])
    paid_success = poll_result(client, buyer_token, paid_activity["id"], {"SUCCESS"})
    paid_order = query_order(client, buyer_token, paid_success["orderNo"])
    paid_code = query_code(client, buyer_token, paid_success["orderNo"])

    export_task = create_export_task(client, publisher_token, paid_activity["id"])
    export_status = poll_export_task(client, publisher_token, export_task["id"])
    downloaded_file = ""
    if export_status.get("status") == "SUCCESS" and export_status.get("fileUrl"):
        file_name = Path(parse.urlparse(export_status["fileUrl"]).path).name
        target_file = output_dir / file_name
        client.download(export_status["fileUrl"], publisher_token, target_file)
        downloaded_file = str(target_file)

    report = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "baseUrl": args.base_url,
        "activities": {
            "free": {
                "id": free_activity["id"],
                "title": free_activity["title"],
            },
            "paid": {
                "id": paid_activity["id"],
                "title": paid_activity["title"],
            },
            "paidImport": {
                "batchNo": import_result.get("batchNo"),
                "successCount": import_result.get("successCount"),
                "failedCount": import_result.get("failedCount"),
            },
        },
        "freeFlow": {
            "attempt": free_attempt,
            "result": free_result,
            "order": free_order,
            "code": free_code,
        },
        "paidFlow": {
            "attempt": paid_attempt,
            "pendingResult": paid_pending,
            "payment": create_payment_result,
            "successResult": paid_success,
            "order": paid_order,
            "code": paid_code,
        },
        "exportFlow": {
            "task": export_task,
            "taskStatus": export_status,
            "downloadedFile": downloaded_file,
        },
        "issues": [],
    }

    json_path = output_dir / "task9-smoke-report.json"
    md_path = output_dir / "task9-smoke-report.md"
    write_json(json_path, report)
    write_smoke_markdown(md_path, report)
    print(f"Smoke acceptance report written to {json_path}")
    print(f"Smoke acceptance markdown written to {md_path}")


def login_many(client: FlashSaleClient, prefix: str, count: int, password: str) -> list[tuple[str, str]]:
    users: list[tuple[str, str]] = []
    for index in range(1, count + 1):
        username = f"{prefix}{index:03d}"
        token = login(client, username, password)
        users.append((username, token))
    return users


def run_load_test(args: argparse.Namespace) -> None:
    client = FlashSaleClient(args.base_url, timeout=args.timeout)
    output_dir = ensure_dir(ROOT_DIR / "logs" / "task9" / f"load-{now_text()}")
    users = login_many(client, args.username_prefix, args.users, args.password)

    start = time.perf_counter()
    responses: list[dict[str, Any]] = []
    latencies: list[float] = []

    def do_attempt(username: str, token: str) -> dict[str, Any]:
        request_id = f"LOAD-{args.activity_id}-{username}-{uuid.uuid4().hex[:8]}"
        result = client.request_json(
            "POST",
            f"/api/seckill/activities/{args.activity_id}/attempt",
            token=token,
            request_id=request_id,
        )
        return {
            "username": username,
            "requestId": request_id,
            "statusCode": result.status_code,
            "elapsedMs": round(result.elapsed_ms, 2),
            "bodyCode": result.body.get("code"),
            "bodyMessage": result.body.get("message"),
            "bodyData": result.body.get("data"),
            "token": token,
        }

    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        future_map = {
            executor.submit(do_attempt, username, token): username
            for username, token in users
        }
        for future in as_completed(future_map):
            response = future.result()
            latencies.append(response["elapsedMs"])
            responses.append(response)

    duration = time.perf_counter() - start
    accepted = [
        response for response in responses
        if response["statusCode"] == 200 and response["bodyCode"] == "SECKILL_PROCESSING"
    ]

    final_results: list[dict[str, Any]] = []
    for response in accepted:
        result = poll_result(
            client,
            response["token"],
            args.activity_id,
            {"SUCCESS", "FAIL", "PENDING_PAYMENT"},
            timeout_seconds=args.poll_timeout,
        )
        final_results.append({
            "username": response["username"],
            "status": result.get("status"),
            "orderNo": result.get("orderNo"),
            "message": result.get("message"),
        })

    summary = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "baseUrl": args.base_url,
        "activityId": args.activity_id,
        "users": args.users,
        "concurrency": args.concurrency,
        "durationSeconds": round(duration, 3),
        "qps": round(len(responses) / duration, 2) if duration > 0 else 0.0,
        "latencyMs": {
            "min": round(min(latencies), 2) if latencies else 0.0,
            "avg": round(statistics.mean(latencies), 2) if latencies else 0.0,
            "p50": percentile(latencies, 0.50),
            "p95": percentile(latencies, 0.95),
            "p99": percentile(latencies, 0.99),
            "max": round(max(latencies), 2) if latencies else 0.0,
        },
        "attemptCodes": aggregate_counts(response["bodyCode"] for response in responses),
        "finalStatuses": aggregate_counts(result["status"] for result in final_results),
        "responses": [
            {
                key: value for key, value in response.items()
                if key != "token"
            }
            for response in responses
        ],
        "finalResults": final_results,
    }

    json_path = output_dir / "task9-load-test-report.json"
    write_json(json_path, summary)
    print(f"Load test report written to {json_path}")


def aggregate_counts(values: Any) -> dict[str, int]:
    counts: dict[str, int] = {}
    for value in values:
        key = str(value)
        counts[key] = counts.get(key, 0) + 1
    return counts


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Task 9 acceptance and load-test helper.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Gateway base url, default http://localhost:18080")
    parser.add_argument("--timeout", type=float, default=15.0, help="HTTP timeout in seconds")
    parser.add_argument("--password", default=DEFAULT_PASSWORD, help="Default password for seeded users")

    subparsers = parser.add_subparsers(dest="command", required=True)

    smoke = subparsers.add_parser("smoke", help="Run Task 9 smoke acceptance flow")
    smoke.add_argument("--publisher-user", default=DEFAULT_PUBLISHER)
    smoke.add_argument("--buyer-user", default=DEFAULT_BUYER)
    smoke.add_argument("--code-file", default=str(DEFAULT_CODE_FILE))
    smoke.set_defaults(func=run_smoke)

    load_test = subparsers.add_parser("load-test", help="Run Task 9 local concurrent seckill test")
    load_test.add_argument("--activity-id", type=int, required=True)
    load_test.add_argument("--users", type=int, default=20)
    load_test.add_argument("--concurrency", type=int, default=20)
    load_test.add_argument("--poll-timeout", type=float, default=12.0)
    load_test.add_argument("--username-prefix", default=DEFAULT_LOAD_USER_PREFIX)
    load_test.set_defaults(func=run_load_test)

    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()

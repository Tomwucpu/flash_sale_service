import argparse
import json
import math
import statistics
import time
import uuid
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


DEFAULT_GATEWAY_URL = "http://localhost:18080"
DEFAULT_DIRECT_URL = "http://localhost:9003"


@dataclass(frozen=True)
class RequestResult:
    success: bool
    http_status: int
    business_code: str
    latency_ms: float
    error: str | None


def resolve_base_url(target: str, override: str | None) -> str:
    if override:
        return override.rstrip("/")
    if target == "gateway":
        return DEFAULT_GATEWAY_URL
    if target == "direct":
        return DEFAULT_DIRECT_URL
    raise ValueError("target must be 'gateway' or 'direct'")


def build_headers(
    user_id: int,
    username: str,
    role: str,
    request_id: str,
    token: str | None,
) -> dict[str, str]:
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-User-Id": str(user_id),
        "X-Username": username,
        "X-Role": role,
        "X-Request-Id": request_id,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def summarize_results(results: list[RequestResult], elapsed_seconds: float) -> dict[str, Any]:
    latencies = [result.latency_ms for result in results]
    status_counts = Counter(result.http_status for result in results)
    code_counts = Counter(result.business_code for result in results)
    success_count = sum(1 for result in results if result.success)
    failed_count = len(results) - success_count

    return {
        "total": len(results),
        "success": success_count,
        "failed": failed_count,
        "elapsed_seconds": round(elapsed_seconds, 3),
        "qps": round(len(results) / elapsed_seconds, 2) if elapsed_seconds > 0 else 0.0,
        "http_status_counts": dict(sorted(status_counts.items())),
        "business_code_counts": dict(sorted(code_counts.items())),
        "latency_ms": latency_summary(latencies),
        "slowest": slowest_results(results, 5),
    }


def latency_summary(latencies: list[float]) -> dict[str, float]:
    if not latencies:
        return {"min": 0.0, "avg": 0.0, "p50": 0.0, "p95": 0.0, "p99": 0.0, "max": 0.0}
    sorted_latencies = sorted(latencies)
    return {
        "min": round(sorted_latencies[0], 2),
        "avg": round(statistics.mean(sorted_latencies), 2),
        "p50": percentile(sorted_latencies, 50),
        "p95": percentile(sorted_latencies, 95),
        "p99": percentile(sorted_latencies, 99),
        "max": round(sorted_latencies[-1], 2),
    }


def percentile(sorted_values: list[float], percentile_value: int) -> float:
    if not sorted_values:
        return 0.0
    rank = math.ceil((percentile_value / 100) * len(sorted_values))
    index = min(max(rank - 1, 0), len(sorted_values) - 1)
    return round(sorted_values[index], 2)


def slowest_results(results: list[RequestResult], limit: int) -> list[dict[str, Any]]:
    return [
        {
            "http_status": result.http_status,
            "business_code": result.business_code,
            "latency_ms": round(result.latency_ms, 2),
            "error": result.error,
        }
        for result in sorted(results, key=lambda item: item.latency_ms, reverse=True)[:limit]
    ]


def run_stress_test(args: argparse.Namespace) -> dict[str, Any]:
    base_url = resolve_base_url(args.target, args.base_url)
    url = f"{base_url}/api/seckill/activities/{args.activity_id}/attempt"
    token = args.token or login_for_token(base_url, args.login_username, args.login_password, args.timeout)

    started_at = time.perf_counter()
    results: list[RequestResult] = []
    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [
            executor.submit(send_attempt, url, args, token, index)
            for index in range(args.requests)
        ]
        for future in as_completed(futures):
            results.append(future.result())
    elapsed = time.perf_counter() - started_at
    return summarize_results(results, elapsed)


def send_attempt(url: str, args: argparse.Namespace, token: str | None, index: int) -> RequestResult:
    user_offset = index % args.users
    user_id = args.user_id_start + user_offset
    username = f"{args.username_prefix}{user_id}"
    request_id = f"{args.request_id_prefix}-{uuid.uuid4().hex}"
    headers = build_headers(user_id, username, args.role, request_id, token)
    request = Request(url, data=b"{}", headers=headers, method="POST")

    started_at = time.perf_counter()
    try:
        with urlopen(request, timeout=args.timeout) as response:
            body = response.read().decode("utf-8", errors="replace")
            latency_ms = (time.perf_counter() - started_at) * 1000
            business_code = extract_business_code(body, response.status)
            return RequestResult(200 <= response.status < 300, response.status, business_code, latency_ms, None)
    except HTTPError as exception:
        body = exception.read().decode("utf-8", errors="replace")
        latency_ms = (time.perf_counter() - started_at) * 1000
        business_code = extract_business_code(body, exception.code)
        return RequestResult(False, exception.code, business_code, latency_ms, body[:300] or exception.reason)
    except (TimeoutError, URLError, OSError) as exception:
        latency_ms = (time.perf_counter() - started_at) * 1000
        return RequestResult(False, 0, "REQUEST_ERROR", latency_ms, str(exception))


def extract_business_code(body: str, http_status: int) -> str:
    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        return f"HTTP_{http_status}"
    code = payload.get("code")
    return str(code) if code else f"HTTP_{http_status}"


def login_for_token(base_url: str, username: str | None, password: str | None, timeout: float) -> str | None:
    if not username and not password:
        return None
    if not username or not password:
        raise ValueError("--login-username and --login-password must be provided together")

    request_id = f"LOGIN-{uuid.uuid4().hex}"
    payload = json.dumps({"username": username, "password": password}).encode("utf-8")
    request = Request(
        f"{base_url}/api/users/login",
        data=payload,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-Request-Id": request_id,
        },
        method="POST",
    )
    with urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8", errors="replace")
    payload_data = json.loads(body)
    token = payload_data.get("data", {}).get("accessToken")
    if not token:
        raise RuntimeError(f"login response did not contain data.accessToken: {body[:300]}")
    return token


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="High-concurrency stress test for the flash-sale seckill API.")
    parser.add_argument("--target", choices=("gateway", "direct"), default="gateway")
    parser.add_argument("--base-url", help="Override target base URL, for example http://localhost:18080")
    parser.add_argument("--activity-id", type=int, required=True)
    parser.add_argument("--requests", type=int, default=1000)
    parser.add_argument("--concurrency", type=int, default=100)
    parser.add_argument("--users", type=int, default=100)
    parser.add_argument("--user-id-start", type=int, default=2001)
    parser.add_argument("--username-prefix", default="buyer-")
    parser.add_argument("--role", default="USER")
    parser.add_argument("--request-id-prefix", default="STRESS")
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--token", help="Bearer token used by gateway mode.")
    parser.add_argument("--login-username", help="Login username used to fetch a gateway token.")
    parser.add_argument("--login-password", help="Login password used to fetch a gateway token.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.requests <= 0:
        raise SystemExit("--requests must be greater than 0")
    if args.concurrency <= 0:
        raise SystemExit("--concurrency must be greater than 0")
    if args.users <= 0:
        raise SystemExit("--users must be greater than 0")
    if args.target == "gateway" and not (args.token or (args.login_username and args.login_password)):
        print("WARNING: gateway mode requires --token or --login-username/--login-password.")

    summary = run_stress_test(args)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

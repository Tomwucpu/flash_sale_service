import unittest

from scripts import seckill_stress_test as stress


class SeckillStressTestUnitTest(unittest.TestCase):
    def test_resolve_base_url_supports_gateway_direct_and_override(self):
        self.assertEqual(stress.resolve_base_url("gateway", None), "http://localhost:18080")
        self.assertEqual(stress.resolve_base_url("direct", None), "http://localhost:9003")
        self.assertEqual(stress.resolve_base_url("gateway", "http://127.0.0.1:19000/"), "http://127.0.0.1:19000")

    def test_build_headers_adds_user_request_and_optional_token(self):
        headers = stress.build_headers(
            user_id=42,
            username="buyer-42",
            role="USER",
            request_id="REQ-42",
            token="abc.def",
        )

        self.assertEqual(headers["X-User-Id"], "42")
        self.assertEqual(headers["X-Username"], "buyer-42")
        self.assertEqual(headers["X-Role"], "USER")
        self.assertEqual(headers["X-Request-Id"], "REQ-42")
        self.assertEqual(headers["Authorization"], "Bearer abc.def")

    def test_summarize_results_calculates_latency_and_code_counts(self):
        results = [
            stress.RequestResult(True, 200, "SECKILL_PROCESSING", 10.0, None),
            stress.RequestResult(True, 200, "OUT_OF_STOCK", 20.0, None),
            stress.RequestResult(False, 500, "HTTP_500", 100.0, "server error"),
        ]

        summary = stress.summarize_results(results, elapsed_seconds=0.5)

        self.assertEqual(summary["total"], 3)
        self.assertEqual(summary["success"], 2)
        self.assertEqual(summary["failed"], 1)
        self.assertEqual(summary["http_status_counts"], {200: 2, 500: 1})
        self.assertEqual(summary["business_code_counts"], {"SECKILL_PROCESSING": 1, "OUT_OF_STOCK": 1, "HTTP_500": 1})
        self.assertEqual(summary["qps"], 6.0)
        self.assertEqual(summary["latency_ms"]["avg"], 43.33)
        self.assertEqual(summary["latency_ms"]["p50"], 20.0)
        self.assertEqual(summary["latency_ms"]["p95"], 100.0)
        self.assertEqual(summary["latency_ms"]["p99"], 100.0)


if __name__ == "__main__":
    unittest.main()

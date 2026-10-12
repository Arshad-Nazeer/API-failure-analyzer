export const SAMPLE_LOGS = `2026-10-13 10:42:12 ERROR PaymentService - Payment gateway timeout for /api/payments
2026-10-13T10:42:13Z GET /api/users 500 145ms
{"timestamp":"2026-10-13T10:42:14Z","level":"ERROR","method":"POST","endpoint":"/api/orders","status":503,"durationMs":2000,"message":"Service unavailable"}
2026-10-13 10:42:15 ERROR OrderService - NullPointerException at OrderProcessor.java:87
GET /api/products 404
POST /api/orders 500
GET /api/users/123 200 45ms
GET /api/users/456 200 52ms
DELETE /api/posts/789 401
2026-10-13 10:42:20 WARN AuthService - 429 Too Many Requests for /api/auth/login
GET /api/payments 500 3200ms
POST /api/payments 500 4100ms
GET /api/payments 500 2800ms
GET /api/health 200 12ms
POST /api/users 403
2026-10-13 10:42:25 ERROR GatewayService - Connection refused: downstream service at /api/inventory
GET /api/reports 502 890ms
PUT /api/config 500
{"timestamp":"2026-10-13T10:42:30Z","level":"WARN","method":"GET","endpoint":"/api/dashboard","status":401,"durationMs":23,"message":"Unauthorized - token expired"}
2026-10-13 10:42:31 ERROR PaymentService - NullPointerException at PaymentService.java:142
Connection timed out: /api/external/provider
GET /api/items/99999 404
POST /api/webhook 500 567ms
GET /api/metrics 200 8ms`;

export const SAMPLE_LOG_DESCRIPTION = "Mixed-format sample: 23 log lines with HTTP 5xx errors, 4xx auth failures, timeouts, exceptions, and successful requests.";

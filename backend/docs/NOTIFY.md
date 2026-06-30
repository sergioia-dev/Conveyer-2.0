# Notify API

Base path: `/api/notify`  
API version header: `X-API-Version: 1`

---

## POST /api/notify/verify

Send a verification code via the specified notification channel (email, SMS). The code is either provided in the request body or auto-generated as a 6-digit number.

### Request Headers

| Header | Value | Required | Description |
|---|---|---|---|
| `X-API-Version` | `1` | Yes | API version |
| `Content-Type` | `application/json` | Yes | |
| `Authorization` | `Bearer sk-<jwt>` | Yes | Project API key (JWT signed with `API_KEY_SECRET`) |

### Request Body

```json
{
  "to": "user@example.com",
  "code": "123456",
  "notificationType": "EMAIL"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `to` | `string` | Yes | Recipient identifier (email address for `EMAIL`, phone number for `SMS`) |
| `code` | `string` | No | Verification code. If omitted or blank, a random 6-digit code is generated |
| `notificationType` | `NotificationType` | Yes | Notification channel: `"EMAIL"` or `"SMS"` |

### Success Response

**Status:** `200 OK`

```json
{
  "code": "123456",
  "sent": true
}
```

| Field | Type | Description |
|---|---|---|
| `code` | `string` | The verification code (provided or auto-generated) |
| `sent` | `boolean` | Always `true` on success |

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "to is required"
}
```

Returned when `to` is `null` or blank.

---

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "notificationType is required"
}
```

Returned when `notificationType` is `null`.

---

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported notification type: ..."
}
```

Returned when `notificationType` is an unrecognised value.

---

**Status:** `401 Unauthorized`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "API key is required"
}
```

Returned when the `Authorization` header is missing, blank, or does not start with `Bearer `.

---

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid API key"
}
```

Returned when the API key cannot be parsed, has an invalid signature, or contains a malformed project ID.

---

**Status:** `404 Not Found`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found"
}
```

Returned when the project referenced by the API key no longer exists.

---

**Status:** `502 Bad Gateway`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Failed to send notification"
}
```

Returned when the notification channel (email, SMS) fails to deliver the message.

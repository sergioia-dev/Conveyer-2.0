# Project API

Base path: `/api/project`  
API version header: `X-API-Version: 1`

---

## POST /api/project/create

Create a new project for the authenticated user. Project names are unique per user.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |
| `Content-Type` | `application/json` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `accessToken` | Yes | JWT access token (set by `/api/auth/signin`) |

### Request Body

```json
{
  "name": "string",
  "domain": "string",
  "description": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | `string` | Yes | Project display name (unique per user) |
| `domain` | `string` | No | Domain associated with the project |
| `description` | `string` | No | Project description |

### Success Response

**Status:** `201 Created`

```json
{
  "id": "uuid",
  "name": "string",
  "domain": "string",
  "description": "string",
  "created_at": "2026-06-30T12:00:00.000"
}
```

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing parameters"
}
```

Returned when `name` is `null` or empty.

---

**Status:** `401 Unauthorized`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Not authenticated"
}
```

Additional 401 responses with the same format use different `message` values:

| `message` | Condition |
|---|---|
| `"Not authenticated"` | Cookie missing or token validation fails |
| `"Invalid token"` | Token passes validation but cannot be parsed |
| `"User not found"` | Token is valid but the user no longer exists |

---

**Status:** `409 Conflict`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 409,
  "error": "Conflict",
  "message": "A project with this name already exists"
}
```

Returned when the user already has a project with the same `name`.

---

## POST /api/project/edit

Update an existing project. Project names are unique per user (the new name must not conflict with another of the user's projects).

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |
| `Content-Type` | `application/json` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `accessToken` | Yes | JWT access token (set by `/api/auth/signin`) |

### Request Body

```json
{
  "id": "uuid",
  "name": "string",
  "domain": "string",
  "description": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `uuid` | Yes | ID of the project to update |
| `name` | `string` | Yes | New project display name (unique per user) |
| `domain` | `string` | No | New domain associated with the project |
| `description` | `string` | No | New project description |

### Success Response

**Status:** `200 OK`

```json
{
  "id": "uuid",
  "name": "string",
  "domain": "string",
  "description": "string",
  "created_at": "2026-06-30T12:00:00.000"
}
```

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing parameters"
}
```

Returned when `name` is `null` or empty.

---

**Status:** `401 Unauthorized`

Same format and messages as `/create`. See the `/create` 401 section for the full list of `message` values.

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

Returned when the project does not exist or does not belong to the authenticated user.

---

**Status:** `409 Conflict`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 409,
  "error": "Conflict",
  "message": "A project with this name already exists"
}
```

Returned when the user already has a different project with the new `name`.

---

## POST /api/project/{projectId}/apikey

Generate (or regenerate) an API key for an existing project. API keys are stored in Redis with an optional TTL.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |
| `Content-Type` | `application/json` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `accessToken` | Yes | JWT access token (set by `/api/auth/signin`) |

### Path Parameters

| Parameter | Type | Description |
|---|---|---|
| `projectId` | `uuid` | ID of the project to generate an API key for |

### Request Body

```json
{
  "expiresInSeconds": 3600
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `expiresInSeconds` | `Long` | No | TTL in seconds. `null` or `0` means the key never expires. Must be non-negative. |

### Success Response

**Status:** `200 OK`

```json
{
  "apiKey": "sk-abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
  "expiresInSeconds": 3600
}
```

| Field | Type | Description |
|---|---|---|
| `apiKey` | `string` | The generated API key (`sk-` prefixed 64-char hex string) |
| `expiresInSeconds` | `Long` | The requested TTL (`null` if not requested; `0` means never expires) |

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-30T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "expiresInSeconds must be non-negative"
}
```

Returned when `expiresInSeconds` is negative.

---

**Status:** `401 Unauthorized`

Same format and messages as `/create`. See the `/create` 401 section for the full list of `message` values.

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

Returned when the project does not exist or does not belong to the authenticated user.

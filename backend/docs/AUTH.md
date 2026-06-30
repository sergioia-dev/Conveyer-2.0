# Authentication API

Base path: `/api/auth`  
API version header: `X-API-Version: 1`

---

## POST /api/auth/signup

Create a new local account.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |
| `Content-Type` | `application/json` | Yes |

### Request Body

```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `username` | `string` | Yes | Display name |
| `email` | `string` | Yes | Email address (used as login identifier) |
| `password` | `string` | Yes | Password (will be encrypted before storage) |

### Success Response

**Status:** `201 Created`

No response body.

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing parameters"
}
```

Returned when any required field is `null` or empty.

---

## POST /api/auth/signin

Authenticate with email and password. Returns JWT access and refresh tokens as HTTP-only cookies.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |
| `Content-Type` | `application/json` | Yes |

### Request Body

```json
{
  "email": "string",
  "password": "string"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `email` | `string` | Yes | Registered email address |
| `password` | `string` | Yes | Account password |

### Success Response

**Status:** `200 OK`

No response body. Tokens are set as HTTP-only cookies:

| Cookie | Path | Max-Age | HttpOnly | SameSite | Secure |
|---|---|---|---|---|---|
| `accessToken` | `/` | 90s | Yes | `Strict` | Yes |
| `refreshToken` | `/api/auth/refresh` | 30d | Yes | `Strict` | Yes |

- `accessToken` — short-lived JWT for authenticating subsequent requests (sent as `Authorization: Bearer <token>`)
- `refreshToken` — long-lived JWT used to obtain a new access token via `/refresh`

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing parameters"
}
```

Returned when `email` or `password` is `null` or empty.

**Status:** `401 Unauthorized`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

Returned when the email doesn't exist in the database, the account provider is not `LOCAL` (e.g., the user signed up via GitHub OAuth), or the password is incorrect.

---

## POST /api/auth/refresh

Exchange a valid refresh token for a new access/refresh token pair. Uses token rotation — each request invalidates the previous refresh token and issues a new one.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `refreshToken` | Yes | Valid refresh token (set by `/signin` or a previous `/refresh`) |

No request body.

### Success Response

**Status:** `200 OK`

No response body. New tokens are set as HTTP-only cookies (same as `/signin`):

| Cookie | Path | Max-Age | HttpOnly | SameSite | Secure |
|---|---|---|---|---|---|
| `accessToken` | `/` | 90s | Yes | `Strict` | Yes |
| `refreshToken` | `/api/auth/refresh` | 30d | Yes | `Strict` | Yes |

### Error Responses

**Status:** `400 Bad Request`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing refresh token"
}
```

Returned when the `refreshToken` cookie is missing or empty.

---

**Status:** `401 Unauthorized`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired refresh token"
}
```

Returned when the refresh token is invalid, expired, malformed, or doesn't match the stored token.

---

## GET /api/auth/status

Check whether the current access token cookie is valid. Returns the authenticated user's email.

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `accessToken` | Yes | JWT access token (set by `/signin` or `/refresh`) |

No request body.

### Success Response

**Status:** `200 OK`

```json
{
  "email": "user@example.com",
  "authenticated": true
}
```

### Error Responses

**Status:** `401 Unauthorized`

```json
{
  "timestamp": "2026-06-11T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Not authenticated"
}
```

Returned when the `accessToken` cookie is missing, invalid, or expired.

---

## GitHub OAuth2 Login

Initiates the OAuth2 authorization code flow with GitHub. On success, the user is redirected back and receives JWT access and refresh tokens as HTTP-only cookies (same as `/signin`).

### Initiate Login

Redirect the browser to:

```
GET /oauth2/authorization/github
```

No headers, cookies, or body required. The browser is redirected to GitHub for authorization.

### Callback (handled by the backend)

After the user authorizes on GitHub, the backend processes the callback at `/login/oauth2/code/github`. This is handled automatically by Spring Security — no manual request needed.

### Success Response

**Status:** `200 OK`

No response body. Tokens are set as HTTP-only cookies:

| Cookie | Path | Max-Age | HttpOnly | SameSite | Secure |
|---|---|---|---|---|---|
| `accessToken` | `/` | 90s | Yes | `Strict` | Yes |
| `refreshToken` | `/api/auth/refresh` | 30d | Yes | `Strict` | Yes |

- `accessToken` — short-lived JWT for authenticating subsequent requests (sent as `Authorization: Bearer <token>`)
- `refreshToken` — long-lived JWT used to obtain a new access token via `/refresh`

### Error Responses

If the OAuth2 flow fails, a JSON error is returned:

```json
{
  "error": "Email not available from GitHub. Ensure your GitHub account has a public email or the user:email scope is granted."
}
```

Other possible errors:

| Error | Cause |
|---|---|
 | `Email not available from GitHub...` | The authenticated GitHub account has no public email or the `user:email` scope was denied |
| `An account with this email already exists...` | A local (email/password) account with the same email already exists |

---

## POST /api/auth/logout

Clears the access and refresh token cookies and invalidates the refresh token in the server (Redis).

### Request Headers

| Header | Value | Required |
|---|---|---|
| `X-API-Version` | `1` | Yes |

### Request Cookies

| Cookie | Required | Description |
|---|---|---|
| `accessToken` | No | Current access token (if present, its associated refresh token is removed from Redis) |

No request body.

### Success Response

**Status:** `200 OK`

No response body. Both cookies are cleared with `maxAge=0`:

| Cookie | Path | Max-Age |
|---|---|---|
| `accessToken` | `/` | `0` (deleted) |
| `refreshToken` | `/api/auth/refresh` | `0` (deleted) |

The endpoint is idempotent — calling it without cookies or with an already-invalidated token still returns `200 OK`.|

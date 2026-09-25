# 💬 Real-Time Chat Application

A real-time chat backend built with **Java, Spring Boot, WebSocket (STOMP), Spring Security (JWT) and PostgreSQL**, following a **Router → Controller → Service → Repository** architecture (functional endpoints, no `@RestController`).

## 🏗️ Architecture pattern

This project mirrors the pattern used in the accompanying sample project:

- **Router** (`router/*.java`) — `@Configuration` classes exposing `RouterFunction<ServerResponse>` beans that map HTTP method + path to a controller method reference.
- **Controller** (`controller/*.java`) — `@Component` classes with methods of signature `ServerResponse handler(ServerRequest request)`. Each method validates input, delegates to a service, and returns a `Map.of("status", ..., "message", ..., "data", ...)` JSON body, with try/catch for expected errors.
- **Service** (`service/*.java`) — business logic and transactions.
- **Repository** (`repository/*.java`) — `JpaRepository` interfaces.
- **WebSocket** (`ws/*.java`) uses Spring's STOMP `@Controller`/`@MessageMapping` model, since that's how Spring WebSocket messaging works (it doesn't use `RouterFunction`).

## ✅ Validation

Functional router endpoints (`RouterFunction`/`ServerRequest`) are **not** processed by Spring MVC's `@Valid` argument resolution the way `@RequestBody @Valid` is on annotated controllers. To keep validation working the same way it would with annotated controllers:

- DTOs (`dto/*.java`) carry standard `jakarta.validation` annotations (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@NotNull`).
- `util/RequestValidator` manually runs the `Validator` bean against a DTO and throws a `ValidationException` (a `Map<field, message>`) on failure.
- Every controller method calls `requestValidator.validate(dto)` right after binding the request body, and catches `ValidationException` to return a `400` with per-field errors:

```json
{
  "status": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email must be a valid email address",
    "password": "Password must be at least 8 characters long"
  }
}
```

- `GlobalExceptionHandler` (`@RestControllerAdvice`) is a safety net for anything not already caught inline.

## 🚀 Features

- User registration & login with BCrypt password hashing and JWT issuance
- Stateless JWT authentication on REST endpoints (`Authorization: Bearer <token>`)
- JWT authentication on WebSocket STOMP `CONNECT` frames too (`JwtChannelInterceptor`)
- One-to-one conversations (auto-reuses an existing direct conversation between two users)
- Real-time message delivery via STOMP over WebSocket (`/topic/conversation/{id}`)
- Typing indicator broadcast (`/topic/conversation/{id}/typing`)
- Paginated message history, mark-as-read, unread counts
- User search by username
- **Voice & video calls** (WebRTC, peer-to-peer media; the server only relays signaling over STOMP)
- **Voice messages** recorded in the browser, with an inline player
- **Sharing files**: images, video (mp4/webm/mov), audio (mp3/wav/ogg/m4a/aac) and documents (pdf, txt, csv, Word, Excel, PowerPoint), up to 50 MB
- **Profile images** (avatars): click the 👤 button in the header to upload and change your profile picture
- Centralized error handling with field-level validation errors

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Backend programming |
| Spring Boot 3.3 | Backend framework |
| Spring Web (functional/`WebMvc.fn`) | REST APIs |
| Spring WebSocket (STOMP) | Real-time communication |
| Spring Security | Authentication & authorization |
| JJWT | JWT issuance/validation |
| Spring Data JPA / Hibernate | ORM |
| PostgreSQL | Relational database |
| Jakarta Bean Validation | Input validation |
| Maven | Build tool |
| JUnit 5 / H2 | Tests |

## 📁 Project Structure

```
src/main/java/com/example/chat/
├── ChatApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java              # serves /uploads/**
│   ├── SchemaMigration.java        # drops the old message_type CHECK constraint (see "Upgrading")
│   └── WebSocketConfig.java
├── router/
│   ├── AuthRouter.java
│   ├── UserRouter.java
│   ├── ConversationRouter.java
│   ├── MessageRouter.java
│   ├── MediaRouter.java
│   └── CallRouter.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ConversationController.java
│   ├── MessageController.java
│   ├── MediaController.java
│   └── CallController.java         # STUN/TURN config for calls
├── ws/
│   ├── ChatWebSocketController.java
│   ├── CallSignalingController.java # WebRTC signaling relay
│   └── JwtChannelInterceptor.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ConversationService.java
│   ├── MessageService.java
│   ├── FileStorageService.java
│   └── CallService.java
├── repository/
│   ├── UserRepository.java
│   ├── ConversationRepository.java
│   └── MessageRepository.java
├── entity/
│   ├── User.java
│   ├── Conversation.java
│   └── Message.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── MessageRequest.java
│   ├── CreateConversationRequest.java
│   ├── CallSignal.java
│   └── TypingRequest.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ValidationException.java
└── util/
    ├── RequestValidator.java
    └── CurrentUserResolver.java

src/main/resources/
├── application.properties
└── static/
    └── index.html          # animated chat UI, served at http://localhost:8080
```

## 🎨 Frontend

A single-file, animated chat UI lives at `src/main/resources/static/index.html`. Spring Boot serves static content automatically, so once the backend is running it's available at **`http://localhost:8080`** — no separate server, no CORS setup needed (same-origin).

It's plain HTML/CSS/JS (no build step) and talks to the backend using:
- `fetch()` for the REST endpoints below (JWT stored in `localStorage`)
- STOMP over SockJS (via CDN) for `/ws`, authenticating the `CONNECT` frame with `Authorization: Bearer <token>`

Features: animated auth screens, live conversation list with unread previews, message bubbles with entrance animation, an animated typing indicator, a pulsing online-status dot, and a live/reconnecting connection badge. Chats also support voice/video calls (phone and camera buttons in the chat header), voice messages (microphone button in the composer — it swaps with the send arrow once you start typing), and attachments (paperclip: photos, video, mp3/audio, documents). If you'd rather host the frontend separately from the API, change the `API_BASE` constant near the top of the `<script>` block in `index.html` to your backend's URL.

## ⚙️ Setup

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a `chat_app` database with user `chat_user` / password `your_password` (see `docker-compose.yml`). Update `src/main/resources/application.properties` if you use different credentials.

### 2. Set a real JWT secret (recommended)

```bash
export JWT_SECRET="a-long-random-string-at-least-32-characters"
export JWT_EXPIRATION=86400000
```

(`application.properties` falls back to a placeholder value if these aren't set — replace it before deploying.)

### 3. Build & run

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Open **`http://localhost:8080`** for the chat UI. The raw API is also available at that same base URL.

## 🔌 REST API

### Auth

```
POST /api/auth/register
{ "username": "john", "email": "john@example.com", "password": "password123" }

POST /api/auth/login
{ "email": "john@example.com", "password": "password123" }
→ { "status": true, "data": { "token": "eyJ..." } }
```

### Users (require `Authorization: Bearer <token>`)

```
GET /api/users/me
GET /api/users/search?username=john
```

### Conversations

```
POST /api/conversations
{ "userId": 2 }

GET /api/conversations
DELETE /api/conversations/{id}
```

### Messages

```
POST /api/messages
{ "conversationId": 10, "content": "Hello!", "messageType": "CHAT" }

GET /api/conversations/{conversationId}/messages?page=0&size=20
POST /api/conversations/{conversationId}/messages/read
```

`messageType` is one of `CHAT, IMAGE, VIDEO, AUDIO, VOICE, FILE` (`JOIN`/`LEAVE` are reserved). Messages with an attachment also carry `attachmentUrl`, `attachmentType`, `attachmentFileName`, `attachmentSize`, and — for `VOICE` — `attachmentDuration` (seconds). `attachmentUrl` must be a URL returned by the upload endpoint below; anything else is rejected.

### Media (require `Authorization: Bearer <token>`)

```
POST /api/media/upload          (multipart/form-data, field "file")
→ { "status": true, "data": { "url": "/uploads/3f2c….mp3", "type": "audio/mpeg",
                              "category": "AUDIO", "fileName": "song.mp3", "size": 4123456 } }
```

Then send a message referencing it:

```
{ "conversationId": 10, "content": "", "messageType": "AUDIO",
  "attachmentUrl": "/uploads/3f2c….mp3", "attachmentType": "audio/mpeg",
  "attachmentFileName": "song.mp3", "attachmentSize": 4123456 }
```

Allowed types: `jpg png gif webp` · `mp4 webm mov` · `mp3 wav ogg m4a aac` (and the `audio/webm` / `audio/mp4` browsers produce when recording) · `pdf txt csv doc docx xls xlsx ppt pptx`. The stored file's extension always comes from the validated content type — never from the client-supplied name — so an uploaded file can't be served back as HTML. Maximum size: 50 MB (`app.upload.max-bytes` and `spring.servlet.multipart.max-file-size`).

### Users

```
GET /api/users/me
→ { "status": true, "data": { "id": 1, "username": "alice", "email": "alice@example.com",
                              "status": "ONLINE", "profileImageUrl": "/uploads/abc123.jpg" } }

PUT /api/users/me                  (application/json)
{ "profileImageUrl": "/uploads/abc123.jpg" }
→ { "status": true, "message": "Profile updated successfully", "data": {...} }
```

The profile image URL (if provided) must be a URL returned by `/api/media/upload` (i.e., starts with `/uploads/`); anything else is rejected for security. To change your profile picture, first upload an image via `/api/media/upload`, then send its URL in a PUT request to `/api/users/me`.

### Calls

```
GET /api/calls/ice-servers
→ { "status": true, "data": { "iceServers": [ { "urls": ["stun:stun.l.google.com:19302"] } ] } }
```

## ⚡ WebSocket (real-time)

- Endpoint: `ws://localhost:8080/ws` (SockJS) or `ws://localhost:8080/ws/websocket` (native WebSocket) — connect and send the JWT as a native STOMP header on `CONNECT`: `Authorization: Bearer <token>`
- Send a message: destination `/app/chat.send`, payload same shape as `POST /api/messages`
- Typing indicator: destination `/app/chat.typing`, payload `{ "conversationId": 10 }`
- Subscribe to receive messages: `/topic/conversation/{conversationId}`
- Subscribe to receive typing events: `/topic/conversation/{conversationId}/typing`
- Subscribe to receive your own send errors: `/user/queue/errors`
- Call signaling: send to `/app/call.signal`, receive on `/user/queue/call` (see below)

### Voice & video calls

Calls use **WebRTC**: audio/video flows directly between the two browsers, and the server only relays the small signaling messages. Payload for `/app/call.signal`:

```json
{ "conversationId": 10, "type": "invite", "callId": "b1f0…", "callType": "video" }
```

| `type` | Sent by | Meaning |
|---|---|---|
| `invite` | caller | start ringing the other participant (`callType`: `audio` or `video`) |
| `accept` / `reject` / `busy` | callee | answer, decline, or "already in a call" |
| `offer` / `answer` | caller / callee | WebRTC session descriptions (`sdp`) |
| `candidate` | both | ICE candidate (`candidate`, a JSON string) |
| `end` | either | hang up / cancel |

Recipients get the same payload on their private `/user/queue/call` queue, plus `fromUserId` and `fromUsername` — always taken from the authenticated session, never from the client. If nobody in the conversation is online when an `invite` is sent, the caller immediately receives `{"type":"unavailable"}` instead of waiting for a timeout. Signals are only relayed between participants of the conversation.

**Requirements & tips**

- Browsers only allow microphone/camera on **HTTPS or `http://localhost`**. To call between different machines, serve the app over HTTPS (e.g. behind a reverse proxy).
- To try it on one machine, sign in as two different users in two different browser profiles (or one normal + one private window) — tabs of the same browser share one login. Use headphones to avoid echo.
- STUN alone works for most home/office networks. Behind strict firewalls or on some mobile networks, calls need a **TURN** relay (e.g. [coturn](https://github.com/coturn/coturn)). Configure via environment variables:

```bash
export TURN_URLS="turn:turn.example.com:3478,turns:turn.example.com:5349"
export TURN_USERNAME="chat"
export TURN_CREDENTIAL="a-long-secret"
```

  (`app.webrtc.stun-urls` can also be changed in `application.properties`.) These credentials are handed to logged-in users; for production prefer short-lived TURN credentials.

## ⬆️ Upgrading an existing database

Adding `AUDIO` and `VOICE` message types, the `attachment_duration` column, and the `profile_image_url` column needs no manual steps: `ddl-auto=update` adds the column, and `config/SchemaMigration` drops the old `messages_message_type_check` constraint that Hibernate created for the previous enum values (otherwise inserting a voice message would fail on a database created before this change). If you'd rather do it by hand:

```sql
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_message_type_check;
```

## 🧪 Testing

```bash
./mvnw test
```

The bundled `ChatApplicationTests` boots the Spring context against an in-memory H2 database so it runs without a live PostgreSQL instance.

## 📈 Possible next steps

Group conversations (and group calls), call history / missed-call messages, desktop or push notifications for incoming calls, screen sharing, message editing/deletion, online presence via WebSocket session events, authorizing `/topic/conversation/{id}` subscriptions per participant, short-lived TURN credentials, Redis-backed message broker (for multi-instance scaling), full OpenAPI/Swagger docs, CI/CD, Dockerfile for the app itself.

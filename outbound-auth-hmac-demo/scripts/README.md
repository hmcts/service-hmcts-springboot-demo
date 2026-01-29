# Curl scripts

**Server** (default `http://localhost:8080`): use `BASE_URL`. **Client** (default `http://localhost:8081`): use `CLIENT_URL`.

```bash
export BASE_URL=http://localhost:8080
export CLIENT_URL=http://localhost:8081
./scripts/curl-health.sh
./scripts/curl-subscribe.sh Alice
```

Scripts:

| Script | Target | Endpoint | Description |
|--------|--------|----------|-------------|
| `curl-health.sh` | Server | GET /actuator/health | Actuator health |
| `curl-get-secret.sh` | **Client** | GET /secret?keyId=... | Get secret from client store only (keyId from subscribe response) |
| `curl-subscribe.sh` | **Client** | POST /subscribe?name=... | Subscribe via client; client calls server (get secret, register), server sends callback (default name=Guest) |
| `curl-rotate-secret.sh` | **Client** | POST /rotate-secret?keyId=... | Rotate secret for keyId; client calls server, stores new secret for future notification verification |
| `curl-notify.sh` | **Server** | POST /api/notify | Notify all subscribers with a message (body `{"message":"..."}`); each notification is signed with that subscriber's secret |

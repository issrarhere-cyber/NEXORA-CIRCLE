# nexora-server

Backend for NEXORA-CIRCLE.

- **Comms**: real-time chat (Socket.IO)
- **Orbit**: text posts and likes (REST)

## Run locally

```
npm install
npm start
```

Server starts on port 3000 (or `PORT` env var).

## API

| Method | Path | Body | What it does |
|---|---|---|---|
| GET | `/` | | status check |
| GET | `/health` | | returns `ok` |
| GET | `/api/posts` | | latest posts, newest first |
| POST | `/api/posts` | `{ "author": "name", "text": "hello" }` | create post |
| POST | `/api/posts/:id/like` | | like a post |
| GET | `/api/messages` | | last 50 chat messages |

## Socket.IO events

- client sends `message` with `{ author, text }` (optional ack callback)
- server broadcasts `message` (new chat message)
- server broadcasts `post` and `post:update` (feed changes)
- server broadcasts `online` (number of connected users)

## Deploy (Render, free)

1. Push this repo to GitHub.
2. Render.com -> New -> Web Service -> pick the repo.
3. Build command: `npm install`, Start command: `npm start`.
4. Use the URL Render gives you (https://....onrender.com) in the app.

Note: free hosting sleeps when idle and may reset `data.json` on restart. A real database comes later.

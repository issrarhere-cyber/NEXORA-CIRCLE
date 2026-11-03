# NEXORA-CIRCLE Architecture

## Technology choices and why

| Layer | Choice | Why |
|---|---|---|
| Android app | Kotlin + Jetpack Compose + Material 3 | Modern standard for Android. Compose makes custom animated space UI easy. |
| Backend | Firebase (Auth, Firestore, Storage, Cloud Messaging, Cloud Functions) | Real hosted backend with no server for you to run. Real-time sync is built in, which messaging needs. Free tier to start, scales later. |
| Authentication | Firebase Authentication (email + password first) | Secure password handling, token refresh and abuse protection are done for you. |
| Database | Cloud Firestore | Real-time listeners for chat and feeds, offline cache, security rules per document. |
| Files | Firebase Storage | Profile pictures, post images and videos, chat files, protected by rules. |
| Push | Firebase Cloud Messaging | Standard push delivery on Android. |
| Build | GitHub Actions | Builds the APK in the cloud, so you do not need a computer. |

## Module layout (current)

```
app/src/main/java/com/nexora/circle/
  MainActivity.kt          app entry point
  ui/
    NexoraApp.kt           bottom navigation and screen switching
    theme/Theme.kt         galaxy colors and Material theme
    components/            StarfieldBackground, GlassCard (reusable)
    screens/               one file per screen
  language/                Nexora-X module (no UI code)
    NexoraXModels.kt       Glyph, NexoraXTable, TranslationResult
    NexoraXRepository.kt   loads assets/nexora_x/*.json
    NexoraXTranslator.kt   both translation directions
app/src/main/assets/nexora_x/   your alphabet and number tables (JSON)
backend/                   Firebase security rules and config
```

Planned modules (added in later stages without rewriting existing ones):
`auth`, `profile`, `feed`, `chat`, `games`, `notifications`, `search`.

## Nexora-X language design

The language is data, not code. Symbols live in JSON files. The keyboard,
translator, lessons and games all read the same loaded table, so supplying the real
tables later changes no Kotlin code. Additional languages later = another folder of
JSON tables plus a language selector.

## Security model (baseline)

- Deny by default; each feature opens only what it needs (`backend/firestore.rules`).
- Users can only edit their own profile, and only allowed fields.
- Chats and messages are readable only by chat members.
- Message content cannot be edited after sending (only read/delivery receipts change).
- Storage uploads are limited by type and size and restricted to the owner's folder.
- Private data (email, push tokens) is kept in a separate owner-only subcollection.

Known limits of the baseline, to be handled before public launch:
- Rules have not yet been run against the Firebase Emulator. This is a required step in Stage 2.
- Firestore rules cannot do rate limiting. Abuse limits will use Cloud Functions and App Check.
- Chat attachment read access is signed-in only; it will be tightened with member checks via Cloud Functions.

## Future features and where they plug in

| Feature | Plan |
|---|---|
| Voice and video calls | WebRTC with Firestore/Functions signaling, new `calls` module |
| AI assistant and alien characters | Cloud Function calling an AI API, so no API keys ship in the app |
| Live streaming | Third-party streaming service, new `live` module |
| Virtual worlds, more games | `games` module with a plug-in game interface |
| Premium subscriptions | Google Play Billing, entitlement stored per user |
| More languages | Additional JSON table folders under `assets/` |

## Delivery plan

1. Stage 1: project, theme, navigation, Nexora-X module, backend rules, cloud APK build
2. Stage 2: Firebase connection, registration, login, profiles
3. Stage 3: posts, likes, comments, follows, stories, search
4. Stage 4: real-time messaging, receipts, presence, push
5. Stage 5: groups and communities, notifications
6. Stage 6: games
7. Stage 7: Nexora-X lessons and games (needs your real tables)

# JevChatKotlin

Native Kotlin Android client for **TypeSafe Jev / System One**.

## Current architecture

- Direct HTTPS requests to the official TypeSafe System One API.
- Uses the public Jev model alias `jev-latest`.
- No Vireonix dependency.
- No OpenAI-compatible chat-completions layer.
- Supports the documented Jev question types: `noul`, `choice`, and `score`.
- Displays Jev structured results, confidence, and probabilities.
- Persists decision history locally in SharedPreferences.
- TypeSafe API key is entered by the user and stored locally so the app can make authenticated requests.
- No WebView or HTML UI.

## Jev API

The app uses:

`POST https://api.typesafe.ai/v1/systemone`

with:

`Authorization: Bearer <API_KEY>`

and `model: "jev-latest"`.

Official API documentation:
https://api.typesafe.ai/docs

Official TypeSafe site:
https://typesafe.ai/

Jev is a System One model for structured decisions, not a string-generation chat model. Its output is typed and includes calibrated probabilities/confidence for supported question types.

## Important

This app is intentionally **Jev-only**. It does not route requests through Vireonix and it does not pretend Jev is a chat-completions model.

## Build

GitHub Actions builds the debug APK as `JevChat-debug`.

The app requires INTERNET and uses HTTPS.

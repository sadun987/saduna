# Ghost Malli AI — Secure Backend & Gemini Setup Guide

Developed by **GHOST MALLI**.

## 1. Default Managed Mode (Firebase AI Logic — Zero Setup Required)
Out of the box, the Android app connects directly through **Firebase AI Logic** (`Firebase.ai`) protected by **Firebase App Check**.
- No Gemini API key is ever stored in Android source code or APK resources.
- Streaming responses, multi-turn conversation history, and structured JSON output work immediately.

## 2. Optional Self-Hosted Express Backend
If you wish to route requests through your own custom backend server:

1. Obtain a Gemini API key from Google AI Studio (`https://aistudio.google.com/apikey`).
2. Navigate to the `backend/` folder:
   ```bash
   cd backend
   npm install
   cp .env.example .env
   ```
3. Edit `backend/.env` and set `GEMINI_API_KEY=...` on the server only.
4. Start the server:
   ```bash
   npm start
   ```
5. In the **Ghost Malli AI** Android app:
   - Open **Settings** -> **Backend Connection**.
   - Switch mode to **Custom Backend API Server**.
   - Enter your server URL (e.g., `http://10.0.2.2:3000` for Android Emulator, or your HTTPS Cloud Run URL).
   - Tap **Test Connection** to verify reachability.

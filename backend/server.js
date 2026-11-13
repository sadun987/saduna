require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const { GoogleGenAI } = require("@google/genai");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

if (!GEMINI_API_KEY) {
  console.error("[FATAL] GEMINI_API_KEY environment variable is missing.");
  process.exit(1);
}

const ai = new GoogleGenAI({ apiKey: GEMINI_API_KEY });

app.use(helmet());
app.use(cors({ origin: process.env.ALLOWED_ORIGIN || "*" }));
app.use(express.json({ limit: "1mb" }));

const limiter = rateLimit({
  windowMs: Number(process.env.RATE_LIMIT_WINDOW_MS || 60000),
  max: Number(process.env.RATE_LIMIT_MAX_REQUESTS || 30),
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Rate limit exceeded. Please wait before sending more requests." },
});

app.use("/api/", limiter);

const ALLOWED_MODELS = new Set([
  "gemini-3.8-flash",
  "gemini-3.5-flash-lite",
  "gemini-4-argon",
]);

app.get("/health", (_req, res) => {
  res.json({ status: "online", service: "Ghost Malli AI Secure Backend" });
});

app.post("/api/chat", async (req, res) => {
  try {
    const { message, history = [], model = "gemini-3.8-flash", language = "Kotlin", replyLanguage = "BILINGUAL" } = req.body || {};

    if (typeof message !== "string" || message.trim().length === 0 || message.length > 40000) {
      return res.status(400).json({ error: "Invalid message payload." });
    }

    const selectedModel = ALLOWED_MODELS.has(model) ? model : "gemini-3.8-flash";
    const safeHistory = Array.isArray(history)
      ? history
          .slice(-16)
          .filter((m) => m && (m.role === "user" || m.role === "model") && typeof m.content === "string")
          .map((m) => ({
            role: m.role,
            parts: [{ text: m.content.slice(0, 20000) }],
          }))
      : [];

    const languageRule =
      replyLanguage === "SINHALA"
        ? "Reply in simple, clear Sinhala while keeping code syntax in English."
        : replyLanguage === "ENGLISH"
        ? "Reply in clear, professional English."
        : "Support both Sinhala and English naturally depending on the user's input.";

    const systemInstruction = [
      "You are GHOST MALLI AI, an elite futuristic dark-fantasy coding assistant developed by GHOST MALLI.",
      `Target code language context: ${String(language).slice(0, 40)}.`,
      languageRule,
      "Provide accurate explanations and well-formatted fenced code blocks.",
    ].join("\n");

    const response = await ai.models.generateContent({
      model: selectedModel,
      contents: [
        ...safeHistory,
        { role: "user", parts: [{ text: message }] },
      ],
      config: { systemInstruction },
    });

    const reply = response.text;
    if (!reply) {
      return res.status(502).json({ error: "Upstream AI returned an empty response." });
    }
    return res.json({ reply });
  } catch (err) {
    console.error("[Backend Error]", err.message || "Request failed");
    return res.status(502).json({ error: "Failed to process AI request." });
  }
});

app.listen(PORT, () => {
  console.log(`Ghost Malli AI Backend listening on port ${PORT}`);
});

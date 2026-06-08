<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=6366F1&height=200&section=header&text=Sangam%20Cosmo%20Browser&fontSize=40&fontColor=ffffff&animation=fadeIn&fontAlignY=35&desc=The%20Ultimate%20AI-Powered%20Android%20Browser&descAlignY=55&descFontSize=16&descFontColor=c7d2fe" />

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-Android%207.0-orange?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

<p align="center">
  <a href="../../releases/latest">
    <img src="https://img.shields.io/github/v/release/SnakeEye-sudo/Sangam-Cosmo-V1?color=6366F1&label=Latest%20Release&style=for-the-badge" />
  </a>
  <a href="../../releases/latest">
    <img src="https://img.shields.io/badge/%E2%AC%87%20Download%20APK-Latest%20Release-F97316?style=for-the-badge" />
  </a>
</p>

---

### 🌌 *Browse the web at the speed of thought — with AI riding shotgun.*

</div>

---

## 📱 About

**Sangam Cosmo Browser** is a feature-packed, privacy-aware Android browser built entirely in Kotlin with Jetpack Compose. It blends the raw power of multi-AI intelligence (Gemini, ChatGPT, Claude) with a sleek sci-fi aesthetic — giving you a browser that doesn't just render the web, it *understands* it.

Whether you're deep in research, casually browsing, downloading media, or locking down your digital footprint — Sangam Cosmo has you covered.

---

## ✨ Features

### 🤖 Multi-AI Intelligence Panel
- **Gemini 2.5 Flash** — AI-powered page summaries, smart Q&A
- **ChatGPT (GPT-4o mini)** — Conversational browsing assistant
- **Claude Haiku** — Precise, thoughtful analysis
- **AI Debate Mode** — Watch AI models debate a topic in real-time
- **Autonomous Research Agent** — Let the AI browse and compile research for you

### 🔍 Sangam Search Ultra
- Custom AI-enhanced search engine
- Aggregates results intelligently
- Context-aware query expansion

### 📥 Smart Media Downloader
- Auto-detects downloadable media on any page
- Status-aware download queue (Pending / Downloading / Completed / Failed)
- Progress tracking with pause/resume
- MediaStore integration — files appear in your Gallery & Downloads

### 🧠 Knowledge & Productivity Tools
- **Knowledge Graph** — Visual map of your browsing insights
- **Memory Vault** — Encrypted personal knowledge base
- **Notes** — Quick in-browser note-taking
- **Task Desk** — Browser-integrated task management
- **Learning Lab** — AI-curated learning paths from your browsing

### 🛡️ Security & Privacy
- AES/GCM encrypted API key storage (Android Keystore)
- Ad Blocker built in
- `allowBackup` disabled — your data stays on your device
- API endpoints forced over HTTPS via Network Security Config

### 🎨 Sci-Fi UI Experience
- Animated SciFi live logo with rotating rings and orbital particles
- Dynamic dark/light theme — **auto-switches at 18:00 IST** (Kolkata timezone)
- Smooth Crossfade transitions between screens
- Material 3 design with custom Cosmo color palette

### 📂 Tab Management
- Multi-tab support with full tab lifecycle management
- Tab state persisted in Room database
- Quick tab switcher overlay

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2 |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Database** | Room 2.7 (7 entities) |
| **Networking** | OkHttp + Retrofit + Moshi |
| **AI APIs** | Gemini API, OpenAI API, Anthropic API |
| **Image Loading** | Coil |
| **Security** | Android Keystore (AES/GCM/NoPadding) |
| **Concurrency** | Kotlin Coroutines + Flow |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 36 (Android 16) |

---

## 📲 Download & Install

> **Latest APK:** [⬇ Download from Releases](../../releases/latest)

### Installation Steps

1. Download the latest `SangamCosmo-vX.X.X.apk` from [Releases](../../releases/latest)
2. On your Android device, go to **Settings → Security → Install unknown apps**
3. Allow your browser/file manager to install APKs
4. Open the downloaded APK and tap **Install**
5. Launch **Sangam Cosmo** from your app drawer

**Minimum:** Android 7.0 (Nougat) | **Recommended:** Android 10+

---

## 🛠️ Build from Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK (API 36)

### Setup

```bash
# Clone the repository
git clone https://github.com/SnakeEye-sudo/Sangam-Cosmo-V1.git
cd Sangam-Cosmo-V1

# Create your .env file with API keys
cp .env.example .env
```

Edit `.env` and fill in your API keys:
```env
GEMINI_API_KEY=your_gemini_api_key_here
OPENAI_API_KEY=your_openai_api_key_here
ANTHROPIC_API_KEY=your_anthropic_api_key_here
```

### Build

```bash
# Debug APK (for testing)
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

---

## 🔐 API Keys

| Key | Purpose | Get It |
|---|---|---|
| `GEMINI_API_KEY` | Page summaries, AI search, Gemini chat | [Google AI Studio](https://aistudio.google.com/apikey) |
| `OPENAI_API_KEY` | ChatGPT panel (optional) | [OpenAI Platform](https://platform.openai.com/api-keys) |
| `ANTHROPIC_API_KEY` | Claude panel (optional) | [Anthropic Console](https://console.anthropic.com/) |

Keys are stored using **Android Keystore AES-256 encryption** — they never leave your device in plaintext.

---

## 🤝 Contributing

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 👨‍💻 Author

**Sangam** — [@SnakeEye-sudo](https://github.com/SnakeEye-sudo)

*Full-stack developer | Android enthusiast | Data analyst*

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=6366F1&height=100&section=footer" />

*Built with 💜 in Kotlin — Powered by curiosity, fueled by caffeine.*

</div>

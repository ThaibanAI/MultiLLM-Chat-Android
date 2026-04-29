# MultiLLM Chat 🤖

**A native Android chat application that lets you talk to multiple LLM providers simultaneously in a single unified interface.**

Send one prompt to **Claude**, **GPT-4o**, and **DeepSeek** at the same time and compare their responses side by side.

---

## ✨ Features

### 🔌 Multi-Provider Support
- **Anthropic Claude** — via Messages API
- **OpenAI** — via Chat Completions API
- **DeepSeek** — via Chat API
- Select any combination of providers per message
- Configure API keys and models per provider

### 💬 Rich Chat Interface
- Traditional chat UI (user right, AI left)
- Provider-labeled response cards with model name
- **Streaming responses** — see text arrive in real-time
- Full **Markdown rendering** with syntax-highlighted code blocks
- Copy, Delete, and Re-send options per message

### 📎 File Attachments
- **Images** — from camera or gallery, sent as base64 to vision-capable providers (Claude, GPT-4o)
- **Documents** — PDF, TXT, and DOCX text extraction
- Graceful handling: non-vision providers skip images with inline notification

### 🗂️ Conversation Management
- Multiple named conversation threads
- Messages persist locally via Room database
- Rename, delete, or clear conversations
- Auto-naming from first message

### ⚙️ Settings
- API key management per provider (encrypted storage)
- Model selection with dropdown presets
- Theme: Light / Dark / System default
- Clear all conversation data

### 🔒 Security & Privacy
- **No backend server** — all API calls go directly from your device to the LLM provider
- API keys stored in **Android EncryptedSharedPreferences** (AES-256 GCM)
- **No analytics, no telemetry, no third-party tracking**

### 🎨 UI
- Jetpack Compose + Material Design 3 (Material You)
- Responsive layout for phones, tablets, and landscape — adaptive bubble widths, keyboard-aware layout
- Smooth animations and transitions
- **Keyboard-aware** — input bar stays above the virtual keyboard; message list is pushed up with `imePadding()`

---

## 📸 Screenshots

> *Screenshots coming soon. Build the app and see for yourself!*

| Conversations | Chat | Settings |
|:---:|:---:|:---:|
| *(add screenshot)* | *(add screenshot)* | *(add screenshot)* |

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | **Kotlin** |
| UI | **Jetpack Compose** + **Material 3** |
| HTTP Client | **Retrofit** + **OkHttp** (SSE streaming) |
| Markdown | **Markwon** + Prism4j |
| Local DB | **Room** |
| DI | **Hilt** |
| Image Loading | **Coil** |
| Security | **AndroidX Security (EncryptedSharedPreferences)** |
| Min SDK | **26** (Android 8.0) |
| Target SDK | **35** |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 35

### Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/ThaibanAI/MultiLLM-Chat-Android.git
   ```

2. **Open in Android Studio**
   - File → Open → select the cloned directory
   - Wait for Gradle sync to complete

3. **Build**
   ```bash
   ./gradlew assembleDebug
   ```
   APK location: `app/build/outputs/apk/debug/app-debug.apk`

4. **Run on device**
   - Connect your Android device (USB debugging enabled) or start an emulator
   - Click Run ▶️ in Android Studio

### Download APK
Download the latest APK from the [Releases page](https://github.com/ThaibanAI/MultiLLM-Chat-Android/releases).

---

## 🔑 API Key Configuration

You need API keys from the providers you want to use:

1. **Anthropic Claude**: Get your key from [console.anthropic.com](https://console.anthropic.com/)
2. **OpenAI**: Get your key from [platform.openai.com](https://platform.openai.com/api-keys)
3. **DeepSeek**: Get your key from [platform.deepseek.com](https://platform.deepseek.com/)

Enter these keys in the app's **Settings** screen. Keys are encrypted and stored locally on your device.

### Supported Models

| Provider | Default Model | Other Options |
|----------|--------------|---------------|
| Claude | `claude-sonnet-4-20250514` | claude-3-5-sonnet, claude-3-5-haiku, claude-3-opus |
| OpenAI | `gpt-4o` | gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo |
| DeepSeek | `deepseek-chat` | deepseek-reasoner |

You can enter any model name manually in the Settings field.

---

## 📁 Project Structure

```
app/
├── src/main/java/com/thaibanai/multillmchat/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/           # Room DAOs
│   │   │   ├── entity/        # Room entities
│   │   │   ├── AppDatabase.kt
│   │   │   └── SecureStorage.kt
│   │   ├── remote/
│   │   │   ├── api/           # Retrofit API interfaces
│   │   │   ├── model/         # Request/Response models
│   │   │   ├── interceptor/   # OkHttp interceptors
│   │   │   └── StreamingService.kt
│   │   └── repository/
│   │       └── ChatRepository.kt
│   ├── di/                    # Hilt dependency injection
│   ├── domain/model/          # Domain models
│   ├── ui/
│   │   ├── components/        # Reusable composables
│   │   ├── screens/
│   │   │   ├── chat/          # Chat screen + ViewModel
│   │   │   ├── conversations/ # Conversations list
│   │   │   └── settings/      # Settings screen
│   │   └── theme/             # Material 3 theming
│   └── util/                  # Helper utilities
└── src/main/res/              # Android resources
```

---

## 🤝 Contributing

Contributions are welcome! Please open an issue first to discuss changes.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## ⚠️ Disclaimer

This app makes direct API calls to third-party LLM providers. You are responsible for:
- Your own API key usage and associated costs
- Compliance with each provider's terms of service
- Data you send to these providers

The app does not collect, store, or transmit any of your data to any server other than the LLM providers you explicitly configure.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/ThaibanAI">ThaibanAI</a>
</p>

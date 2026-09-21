# Vidmax — Professional Video Downloader

> Paste any link. Pick quality. Download instantly. Production-ready dark theme.

Vidmax is a modern, professional video downloader with a **floating bottom navigation** and three core screens. The download engine is fully abstracted — swap the mock engine with your real `yt-dlp`/`ffmpeg` backend without touching UI.

### ✨ Live Preview
Run `npm run dev` — app binds to `0.0.0.0:5173` and works with Arena preview (`https://{port}-{sandboxId}.e2b.app`).

---

## 🧭 Bottom Navigation (3 Screens)

| Tab | Route | Purpose |
|-----|-------|---------|
| **Browser** | `BrowserScreen` | Paste link + in-app browser with auto video detection |
| **Downloads** | `ProgressScreen` | Active queue with live progress, speed, ETA, pause/resume/cancel |
| **Library** | `LibraryScreen` | Completed files — grid/list, search, filter, play/share/delete |

All screens share `DownloadContext` for queue, persistence (`localStorage`), and toasts.

---

## 🎨 Theme — Professional Production Ready

- **Palette**: Deep OLED `#060A14` background, slate surfaces `#0F1423/#161E36`, electric violet `#6C5CFF` → cyan `#00D9FF` gradient, glassmorphism with `backdrop-blur` + subtle borders
- **Typography**: Inter, tight tracking, uppercase micro-labels
- **Components**: Rounded `2xl` cards, floating pill bottom nav, gradient CTAs, glowing shadows, shimmer progress bars
- **Details**: storage ring, live throughput graph, empty states with CTAs, quality bottom sheet, detail drawer, toast stack

---

## 🚀 Quick Start

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run preview
```

---

## 🧠 Engine (Keep Same, Swap Backend)

`src/engine/downloadEngine.js`

- `detectPlatform(url)` / `isValidUrl(url)` / `getPlatformInfo(key)` — platform detection
- `mockFetchMetadata(url)` — simulates `yt-dlp --dump-json` (replace with `fetch('/api/metadata?url=')`)
- `parseSizeToMB(size)` — size parsing for ETA/speed

`src/context/DownloadContext.jsx`

- `addDownload(meta, quality)` — queues & auto-starts (max 2 parallel)
- `pauseDownload` / `resumeDownload` / `cancelDownload` / `retryDownload` / `deleteDownload`
- Simulated progress loop (speed 1.5–6 MB/s, ETA calculation) — replace interval with real IPC/WebSocket progress from backend
- Persists to `localStorage` (`vidmax_downloads_v2`)

> **To go production**: point `mockFetchMetadata` and the interval to your backend (Node/Go/Python `yt-dlp` + `ffmpeg` merge). UI needs zero changes.

---

## 🖥️ Screen Details

### 1. Browser — Paste & Browse
- Large **Paste** input with Paste button (Clipboard API), clear, validation, Enter-to-fetch
- Preview card: thumbnail, duration, platform badge, uploader, viewCount, quality selector, size, Download Now
- **Quality sheet**: Video (2160p → 480p) + Audio (MP3/M4A) with Recommended badge
- Popular sites grid (8 platforms) → opens in embedded browser
- **In-app browser**: address bar, secure badge, video-detected floating pill, mock YouTube grid where tapping a tile auto-pastes download link
- History chips, ShieldCheck secure footer, How-it-works 3-step cards

### 2. Downloads — Progress
- Top stats: Active count, live total speed, storage ring (64 GB mock)
- Filters: All / Downloading / Queued / Paused
- Each item: thumbnail + status pill + speed or quality, progress bar with shimmer, ETA, platform/duration, pause/resume/cancel/retry
- Live throughput sparkline + tip card + smart-queue note (2 parallel max, auto-resume)

### 3. Library — Completed
- Header: file count, total size, clear all, grid/list toggle
- Search + sort (date/size/name) + platform filter chips
- **Grid**: card with gradient overlay, Play on hover, format/size/date, Play/Share/Delete
- **List**: horizontal row with Show in folder
- Empty states, filter empty state, detail drawer (thumbnail hero, stats, Play/Share/Delete, location/format/saved/source, Rename/Show in Files)

---

## ➕ Improvements Added Beyond Request

- **Auto clipboard detection**, history persistence, platform auto-detect
- **Concurrent queue (2 max)** with FIFO, pause/resume with retained partial file
- **Storage indicator** + live speed graph
- **Grid/List toggle**, search, platform filtering, sorting
- **Settings drawer** (Wi-Fi only, notifications, dark theme, storage)
- **Toast system**, framer-motion transitions, empty states, skeletons, error handling
- **Responsive**: mobile-first pill nav, safe-area insets, 960px max container, desktop footer
- **PWA-ready** meta, Inter fonts, glass effects

---

## 📁 Structure

```
src/
  engine/downloadEngine.js   # engine abstraction
  context/DownloadContext.jsx
  components/
    BottomNav.jsx
    Header.jsx
    PlatformGrid.jsx
  screens/
    BrowserScreen.jsx
    ProgressScreen.jsx
    LibraryScreen.jsx
  App.jsx
  main.jsx
  index.css
```

---

## 🔧 Production Checklist

- [ ] Replace `mockFetchMetadata` with `POST /api/download/metadata`
- [ ] Replace simulated interval with backend progress (WebSocket/SSE)
- [ ] Wire `ffmpeg` mux + file system paths (`/Downloads/Vidmax`)
- [ ] Add native bridge if wrapping with Capacitor/Tauri/Electron
- [ ] Enable PWA service worker

Built with Vite + React + Tailwind + Framer Motion + Lucide.

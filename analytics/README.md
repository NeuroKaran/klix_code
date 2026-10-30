# 📊 Umami Analytics for Klix Code
> Privacy-friendly, 100% free analytics for website visits and unique download tracking.

---

## 🎯 What This Solves

1. **Total & Unique Website Visits**: Tracks pageviews, unique visitors, referrers, bounce rate, geography, device type, and OS without cookies or invasive tracking banners.
2. **Unique Downloads (vs. Total Downloads)**: 
   - A user who downloads, uninstalls, and re-downloads the APK will trigger **multiple download events**, but Umami automatically records them as **1 unique visitor / downloader**.
   - You get **both numbers**:
     - **Total Downloads**: Total times the APK link was clicked.
     - **Unique Downloads**: Deduplicated count of individual users who downloaded.

---

## 📁 Directory Structure

```
analytics/
├── README.md               # Complete setup, deployment & dashboard guide (this file)
├── events-schema.json      # Structured definition of all tracked events & metadata
├── tracker.js              # Reusable Umami client with safe event firing & fallback
└── .env.example            # Environment variables template for website ID & host
```

---

## 🚀 Quick Setup (Choose Option A or Option B)

### Option A: Umami Cloud (Recommended — 2 Minutes, Zero Maintenance)
*Free tier includes 3 websites and 10,000 monthly events with zero server management.*

1. **Sign up**: Go to [cloud.umami.is](https://cloud.umami.is/) and create a free account.
2. **Add Website**:
   - Name: `Klix Code`
   - Domain: `klixcode.vercel.app`
3. **Copy Website ID**:
   - Copy the generated `Website ID` (UUID format, e.g. `b4e6a8d2-1234-4567-8901-abcdef123456`).
4. **Update `landing_page/index.html`**:
   - Replace `YOUR_UMAMI_WEBSITE_ID` in the `<script>` tag:
     ```html
     <script
       defer
       src="https://cloud.umami.is/script.js"
       data-website-id="YOUR_UMAMI_WEBSITE_ID"
     ></script>
     ```

---

### Option B: Self-Hosted on Vercel + Free Neon/Supabase (100% Free Forever, Unlimited)
*Zero limits on pageviews or events. Runs on your existing Vercel account.*

1. **Create a Free PostgreSQL Database**:
   - Go to [neon.tech](https://neon.tech) or [supabase.com](https://supabase.com) (free tier, no credit card required).
   - Create a project and copy the connection string:
     `postgresql://username:password@ep-sample-12345.us-east-2.aws.neon.tech/neondb?sslmode=require`

2. **Deploy Umami to Vercel**:
   - Go to [Umami GitHub](https://github.com/umami-software/umami).
   - Click the **"Deploy with Vercel"** button.
   - Configure environment variables in Vercel:
     - `DATABASE_URL`: Your Neon/Supabase connection string.
     - `APP_SECRET`: Any random 32-character string.
   - Click **Deploy**. Vercel will create your Umami dashboard at e.g. `https://umami-klix.vercel.app`.

3. **Login & Create Website**:
   - Open your deployed Umami URL.
   - Default login: `admin` / `umami` (change this immediately under Settings -> Profile).
   - Go to **Settings → Websites → Add Website**.
   - Copy the `Website ID`.

4. **Update `landing_page/index.html`**:
   ```html
   <script
     defer
     src="https://umami-klix.vercel.app/script.js"
     data-website-id="YOUR_UMAMI_WEBSITE_ID"
   ></script>
   ```

---

## 📥 How Unique Download Tracking Works

### 1. Dual Tracking Mechanism
To ensure 100% accuracy, tracking is handled in two ways:

- **Declarative HTML attributes**:
  ```html
  <a href="/klix-code-v0.2.1.apk"
     download="klix-code-v0.2.1.apk"
     data-umami-event="apk_download"
     data-umami-event-version="v0.2.1"
     data-umami-event-source="hero">
     Download APK
  </a>
  ```
  *Umami's script automatically intercepts elements with `data-umami-event`.*

- **Programmatic tracker (`tracker.js`)**:
  ```javascript
  import { trackDownload } from './tracker.js';

  trackDownload({
    version: 'v0.2.1',
    source: 'hero', // 'hero' | 'main_card' | 'mobile_nav' | 'footer'
    fileName: 'klix-code-v0.2.1.apk',
  });
  ```

### 2. Differentiating Unique vs. Reinstalls in Umami Dashboard
In the Umami dashboard under **Events → `apk_download`**:
- **Visitors column**: The number of unique individuals who downloaded the app. Even if a user clicks download 5 times or uninstalls and re-downloads later that day, **Visitors = 1**.
- **Events column**: Total raw download clicks (including repeat/reinstall downloads).
- **Properties breakdown**: Click on the event to see a pie chart of download sources (`hero`, `main_card`, `footer`, etc.) and app versions.

---

## 📋 Tracked Events Reference

| Event Name | Properties | Description |
|---|---|---|
| `pageview` | (Auto-tracked by Umami) | Automatic tracking of page URL, referrer, browser, device, OS, country |
| `apk_download` | `version`, `source`, `file_name` | Triggered whenever any APK download button is clicked |
| `copy_checksum` | `algorithm`, `hash_prefix` | Triggered when user copies the APK SHA-256 hash |
| `demo_interaction` | `tab_name`, `action` | Triggered when interacting with interactive phone mockups |

---

## 🧪 Testing Locally

When running `npm run dev` in `landing_page/`:
1. By default, Umami ignores `localhost` to prevent polluting production data.
2. To test locally, you can pass `data-domains="localhost,klixcode.vercel.app"` to the script tag, or inspect the console where `tracker.js` logs:
   `[Umami Analytics] Event tracked: apk_download { version: "v0.2.1", source: "hero" }`

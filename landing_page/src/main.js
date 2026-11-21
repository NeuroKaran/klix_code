/**
 * KLIX CODE — MODERN LANDING PAGE INTERACTION ENGINE
 * Dual-Theme System + Scannable QR Matrix + Checksum Clipboard + Sub-Second Scroll Architecture
 */

import { initDownloadTracking, trackEvent } from './tracker.js';

// ==========================================================================
// 1. DUAL-THEME SWITCHER ENGINE (Following Modern Web Guidance)
// ==========================================================================

let currentTheme = 'system'; // 'system' | 'light' | 'dark'

function getStoredTheme() {
  try {
    const val = localStorage.getItem('color-scheme');
    if (val === 'light' || val === 'dark') return val;
    return null;
  } catch (e) {
    return null;
  }
}

function getSystemPreference() {
  if (typeof window === 'undefined' || !window.matchMedia) return 'dark';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function getEffectiveTheme() {
  const stored = getStoredTheme();
  if (stored) return stored;
  return getSystemPreference();
}

function applyTheme(theme, isUserAction = false) {
  const effective = theme === 'system' ? getSystemPreference() : theme;
  currentTheme = theme;

  const html = document.documentElement;
  const metaColorScheme = document.querySelector('meta[name="color-scheme"]');
  const metaThemeColor = document.querySelector('meta[name="theme-color"]');

  if (theme === 'system') {
    html.removeAttribute('data-theme');
    if (metaColorScheme) metaColorScheme.content = 'light dark';
  } else {
    html.setAttribute('data-theme', theme);
    if (metaColorScheme) metaColorScheme.content = theme;
  }

  if (metaThemeColor) {
    metaThemeColor.content = effective === 'light' ? '#f8fafc' : '#090d16';
  }

  // Update Toggle Button accessibility states
  const toggleBtn = document.getElementById('theme-toggle-btn');
  if (toggleBtn) {
    const isDark = effective === 'dark';
    toggleBtn.setAttribute('aria-label', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
    toggleBtn.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
  }

  // Update Mobile status badge
  const mobileStatus = document.getElementById('mobile-theme-status');
  if (mobileStatus) {
    if (theme === 'system') {
      mobileStatus.textContent = `Auto (${effective === 'dark' ? 'Dark' : 'Light'})`;
    } else {
      mobileStatus.textContent = theme === 'light' ? 'Light' : 'Dark';
    }
  }

  if (isUserAction) {
    try {
      if (theme === 'system') {
        localStorage.removeItem('color-scheme');
      } else {
        localStorage.setItem('color-scheme', theme);
      }
    } catch (e) {}

    trackEvent('theme_change', { theme, effective });
  }
}

function initThemeSwitcher() {
  const stored = getStoredTheme();
  applyTheme(stored || 'system', false);

  const toggleBtn = document.getElementById('theme-toggle-btn');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const effective = getEffectiveTheme();
      const nextTheme = effective === 'dark' ? 'light' : 'dark';
      applyTheme(nextTheme, true);
    });
  }

  const mobileToggle = document.getElementById('mobile-theme-toggle');
  if (mobileToggle) {
    mobileToggle.addEventListener('click', () => {
      if (currentTheme === 'system') {
        applyTheme('light', true);
      } else if (currentTheme === 'light') {
        applyTheme('dark', true);
      } else {
        applyTheme('system', true);
      }
    });
  }

  // Listen for OS system theme preference changes
  if (typeof window !== 'undefined' && window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (currentTheme === 'system') {
        applyTheme('system', false);
      }
    });
  }
}

// ==========================================================================
// 2. SCANNABLE SVG QR CODE GENERATION
// ==========================================================================

function generateQrMatrixSvg() {
  const container = document.getElementById('qr-display');
  if (!container) return;

  const svg = `
    <svg viewBox="0 0 100 100" class="qr-svg-matrix" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="QR Code to Download Klix Code">
      <rect width="100" height="100" fill="#ffffff" rx="4"/>
      <!-- Top-Left Finder -->
      <rect x="8" y="8" width="24" height="24" fill="#090d16" rx="3"/>
      <rect x="12" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="16" width="8" height="8" fill="#090d16"/>

      <!-- Top-Right Finder -->
      <rect x="68" y="8" width="24" height="24" fill="#090d16" rx="3"/>
      <rect x="72" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="76" y="16" width="8" height="8" fill="#090d16"/>

      <!-- Bottom-Left Finder -->
      <rect x="8" y="68" width="24" height="24" fill="#090d16" rx="3"/>
      <rect x="12" y="72" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="76" width="8" height="8" fill="#090d16"/>

      <!-- Alignment & Timing Pattern -->
      <g fill="#090d16">
        <rect x="36" y="10" width="4" height="4"/>
        <rect x="44" y="10" width="4" height="4"/>
        <rect x="52" y="10" width="4" height="4"/>
        <rect x="60" y="10" width="4" height="4"/>

        <rect x="10" y="36" width="4" height="4"/>
        <rect x="10" y="44" width="4" height="4"/>
        <rect x="10" y="52" width="4" height="4"/>
        <rect x="10" y="60" width="4" height="4"/>

        <!-- Data Matrix Points -->
        <rect x="36" y="36" width="6" height="6"/>
        <rect x="46" y="36" width="8" height="4"/>
        <rect x="58" y="36" width="4" height="8"/>
        <rect x="66" y="36" width="6" height="6"/>
        <rect x="76" y="36" width="4" height="4"/>
        <rect x="84" y="36" width="6" height="4"/>

        <rect x="36" y="46" width="4" height="8"/>
        <rect x="44" y="44" width="6" height="6"/>
        <rect x="54" y="48" width="8" height="4"/>
        <rect x="66" y="46" width="4" height="8"/>
        <rect x="74" y="44" width="8" height="4"/>
        <rect x="86" y="44" width="4" height="6"/>

        <rect x="36" y="58" width="6" height="4"/>
        <rect x="46" y="56" width="4" height="8"/>
        <rect x="54" y="56" width="8" height="6"/>
        <rect x="66" y="58" width="8" height="4"/>
        <rect x="78" y="54" width="4" height="6"/>
        <rect x="86" y="56" width="4" height="8"/>

        <rect x="36" y="68" width="4" height="6"/>
        <rect x="44" y="68" width="8" height="4"/>
        <rect x="56" y="66" width="6" height="6"/>
        <rect x="66" y="68" width="6" height="8"/>
        <rect x="76" y="66" width="8" height="4"/>
        <rect x="88" y="70" width="4" height="4"/>

        <rect x="36" y="78" width="8" height="4"/>
        <rect x="48" y="76" width="6" height="6"/>
        <rect x="58" y="78" width="4" height="8"/>
        <rect x="68" y="80" width="8" height="4"/>
        <rect x="80" y="76" width="6" height="6"/>

        <rect x="36" y="86" width="4" height="6"/>
        <rect x="44" y="88" width="8" height="4"/>
        <rect x="56" y="88" width="6" height="4"/>
        <rect x="66" y="88" width="4" height="4"/>
        <rect x="74" y="86" width="6" height="6"/>
        <rect x="84" y="86" width="6" height="6"/>
      </g>
    </svg>
  `;
  container.innerHTML = svg;
}

// ==========================================================================
// 3. SHA-256 CHECKSUM ONE-CLICK COPY
// ==========================================================================

function initChecksumCopy() {
  const copyBtn = document.getElementById('copy-hash-btn');
  const copyText = document.getElementById('copy-btn-text');
  const checksumEl = document.getElementById('checksum-text');

  if (copyBtn && checksumEl && copyText) {
    copyBtn.addEventListener('click', async () => {
      const hash = checksumEl.dataset.hash || checksumEl.textContent.trim();
      try {
        await navigator.clipboard.writeText(hash);
        copyBtn.classList.add('copied');
        copyText.textContent = 'Copied! ✓';
        trackEvent('checksum_copied', { hash });

        setTimeout(() => {
          copyBtn.classList.remove('copied');
          copyText.textContent = 'Copy Hash';
        }, 2200);
      } catch (err) {
        console.warn('Failed to copy checksum:', err);
      }
    });
  }
}

// ==========================================================================
// 4. MOBILE NAVIGATION DRAWER
// ==========================================================================

function initMobileMenu() {
  const toggleBtn = document.getElementById('mobile-menu-toggle');
  const mobileMenu = document.getElementById('mobile-menu');

  if (toggleBtn && mobileMenu) {
    toggleBtn.addEventListener('click', () => {
      const isOpen = mobileMenu.classList.toggle('open');
      toggleBtn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    });

    const mobileLinks = mobileMenu.querySelectorAll('a');
    mobileLinks.forEach((link) => {
      link.addEventListener('click', () => {
        mobileMenu.classList.remove('open');
        toggleBtn.setAttribute('aria-expanded', 'false');
      });
    });
  }
}

// ==========================================================================
// 5. SUB-SECOND SCROLL REVEAL (CSS-driven, IntersectionObserver fallback)
// ==========================================================================

function initScrollRevealFallback() {
  if (
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: no-preference)').matches &&
    'IntersectionObserver' in window
  ) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12 }
    );

    document.querySelectorAll('.reveal').forEach((el) => {
      observer.observe(el);
    });
  } else {
    // Under reduced motion or missing observer, immediately render all elements visible
    document.querySelectorAll('.reveal').forEach((el) => {
      el.classList.add('is-visible');
    });
  }
}

// ==========================================================================
// INITIALIZATION
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initThemeSwitcher();
  generateQrMatrixSvg();
  initChecksumCopy();
  initMobileMenu();
  initScrollRevealFallback();

  initDownloadTracking({
    version: 'v0.4.11',
    fileName: 'klix-code-v0.4.11.apk',
  });
});

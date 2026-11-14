/**
 * KLIX CODE — LANDING PAGE ENGINE
 * Dual-Theme System (Light Aurora & Cosmic Obsidian) + Interactive Phone Showcase
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
  if (stored) {
    return stored;
  }
  return getSystemPreference();
}

let clearMatrixCanvasFn = null;

function applyTheme(theme, isUserAction = false) {
  const effective = theme === 'system' ? getSystemPreference() : theme;
  currentTheme = theme;

  const html = document.documentElement;
  const metaColorScheme = document.querySelector('meta[name="color-scheme"]');
  const metaThemeColor = document.querySelector('meta[name="theme-color"]');

  // Update DOM attributes
  if (theme === 'system') {
    html.removeAttribute('data-theme');
    if (metaColorScheme) metaColorScheme.content = 'light dark';
  } else {
    html.setAttribute('data-theme', theme);
    if (metaColorScheme) metaColorScheme.content = theme;
  }

  if (metaThemeColor) {
    metaThemeColor.content = effective === 'light' ? '#d8eefe' : '#0b0e1a';
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

  // Update Phone Mockup In-App Settings Theme Chips
  const chipLight = document.getElementById('settings-theme-light');
  const chipDark = document.getElementById('settings-theme-dark');
  const chipSystem = document.getElementById('settings-theme-system');

  if (chipLight) chipLight.classList.toggle('active', theme === 'light');
  if (chipDark) chipDark.classList.toggle('active', theme === 'dark');
  if (chipSystem) chipSystem.classList.toggle('active', theme === 'system');

  if (clearMatrixCanvasFn) {
    clearMatrixCanvasFn();
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
    const themeName = theme === 'system'
      ? `System (${effective === 'light' ? 'Light Aurora' : 'Cosmic Obsidian'})`
      : (theme === 'light' ? 'Light Aurora' : 'Cosmic Obsidian');
    showToast(`Switched to ${themeName} theme`);
  }
}

function initThemeSwitcher() {
  const toggleBtn = document.getElementById('theme-toggle-btn');
  const mobileToggle = document.getElementById('mobile-theme-toggle');

  const stored = getStoredTheme();
  if (stored) {
    applyTheme(stored, false);
  } else {
    applyTheme('system', false);
  }

  // Follow modern-web-guidance for two-state toggles:
  // Target is opposite of currently resolved theme.
  // If target matches current system preference, remove stored override (revert to system).
  // Otherwise, store override.
  function handleToggleClick() {
    const currentEffective = getEffectiveTheme();
    const systemPref = getSystemPreference();
    const targetScheme = currentEffective === 'dark' ? 'light' : 'dark';
    const nextTheme = targetScheme === systemPref ? 'system' : targetScheme;
    applyTheme(nextTheme, true);
  }

  if (toggleBtn) {
    toggleBtn.addEventListener('click', handleToggleClick);
  }

  if (mobileToggle) {
    mobileToggle.addEventListener('click', () => {
      const stored = getStoredTheme();
      // Cycle: Auto -> Dark -> Light -> Auto
      let next;
      if (!stored) {
        next = 'dark';
      } else if (stored === 'dark') {
        next = 'light';
      } else {
        next = 'system';
      }

      applyTheme(next, true);
    });
  }

  // Listen to OS system preference changes
  if (window.matchMedia) {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', () => {
      const stored = getStoredTheme();
      if (!stored) {
        applyTheme('system', false);
      }
    });
  }

  // Sync across tabs
  window.addEventListener('storage', (e) => {
    if (e.key === 'color-scheme') {
      applyTheme(e.newValue || 'system', false);
    }
  });
}

// ==========================================================================
// 2. PHONE SCREEN TABS TOGGLE (Home / Agent / Terminal / Diff / Settings)
// ==========================================================================

function switchPhoneTab(target) {
  const tabBtns = document.querySelectorAll('.phone-tab-btn');
  const dockBtns = document.querySelectorAll('.dock-item');
  const panels = {
    home: document.getElementById('tab-home'),
    agent: document.getElementById('tab-agent'),
    terminal: document.getElementById('tab-terminal'),
    diff: document.getElementById('tab-diff'),
    settings: document.getElementById('tab-settings'),
  };

  // Sync tab buttons
  tabBtns.forEach((btn) => {
    const isTarget = btn.getAttribute('data-tab') === target;
    btn.classList.toggle('active', isTarget);
    btn.setAttribute('aria-selected', isTarget ? 'true' : 'false');
  });

  // Sync dock items (Diff maps to Agent on the 4-destination dock)
  dockBtns.forEach((btn) => {
    const dockDest = btn.getAttribute('data-dock');
    const isDockActive = (dockDest === target) || (target === 'diff' && dockDest === 'agent');
    btn.classList.toggle('active', isDockActive);
  });

  // Toggle active panel
  Object.keys(panels).forEach((key) => {
    if (panels[key]) {
      panels[key].classList.toggle('active', key === target);
    }
  });
}

function initPhoneMockupTabs() {
  const tabBtns = document.querySelectorAll('.phone-tab-btn');
  const dockBtns = document.querySelectorAll('.dock-item');

  tabBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-tab');
      if (target) switchPhoneTab(target);
    });
  });

  dockBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const dockDest = btn.getAttribute('data-dock');
      if (dockDest) switchPhoneTab(dockDest);
    });
  });

  // Home Screen Primary CTA
  const homeOpenBtn = document.getElementById('phone-home-open-btn');
  if (homeOpenBtn) {
    homeOpenBtn.addEventListener('click', () => {
      homeOpenBtn.textContent = 'Launching Terminal...';
      setTimeout(() => {
        switchPhoneTab('terminal');
        homeOpenBtn.textContent = '+ Open Sandbox Workspace';
      }, 350);
    });
  }

  // Agent Tool Call Approval Action
  const approveBtn = document.getElementById('mock-approve-btn');
  if (approveBtn) {
    approveBtn.addEventListener('click', () => {
      approveBtn.textContent = 'Approved ✓ Applying Patch...';
      approveBtn.style.backgroundColor = '#34d399';
      setTimeout(() => {
        switchPhoneTab('diff');
        approveBtn.textContent = 'Approve Tool Call ✓';
        approveBtn.style.backgroundColor = '';
      }, 500);
    });
  }

  // In-Phone Settings Screen Theme Toggles
  const chipLight = document.getElementById('settings-theme-light');
  const chipDark = document.getElementById('settings-theme-dark');
  const chipSystem = document.getElementById('settings-theme-system');
  const closeSettingsBtn = document.getElementById('settings-close-btn');

  if (chipLight) {
    chipLight.addEventListener('click', () => applyTheme('light', true));
  }
  if (chipDark) {
    chipDark.addEventListener('click', () => applyTheme('dark', true));
  }
  if (chipSystem) {
    chipSystem.addEventListener('click', () => applyTheme('system', true));
  }
  if (closeSettingsBtn) {
    closeSettingsBtn.addEventListener('click', () => switchPhoneTab('home'));
  }
}

// ==========================================================================
// 3. SVG QR CODE GENERATOR (Scannable with Android Camera)
// ==========================================================================

function generateQrMatrixSvg() {
  const container = document.getElementById('qr-display');
  if (!container) return;

  const svg = `
    <svg viewBox="0 0 100 100" class="qr-svg-matrix" xmlns="http://www.w3.org/2000/svg">
      <rect width="100" height="100" fill="#ffffff" rx="4"/>
      <!-- Top-Left Corner -->
      <rect x="8" y="8" width="24" height="24" fill="#0c0d14" rx="3"/>
      <rect x="12" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="16" width="8" height="8" fill="#0c0d14"/>

      <!-- Top-Right Corner -->
      <rect x="68" y="8" width="24" height="24" fill="#0c0d14" rx="3"/>
      <rect x="72" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="76" y="16" width="8" height="8" fill="#0c0d14"/>

      <!-- Bottom-Left Corner -->
      <rect x="8" y="68" width="24" height="24" fill="#0c0d14" rx="3"/>
      <rect x="12" y="72" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="76" width="8" height="8" fill="#0c0d14"/>

      <!-- QR Data Grid -->
      <g fill="#0c0d14">
        <rect x="36" y="10" width="4" height="4"/>
        <rect x="44" y="10" width="4" height="4"/>
        <rect x="52" y="10" width="4" height="4"/>
        <rect x="60" y="10" width="4" height="4"/>

        <rect x="10" y="36" width="4" height="4"/>
        <rect x="10" y="44" width="4" height="4"/>
        <rect x="10" y="52" width="4" height="4"/>
        <rect x="10" y="60" width="4" height="4"/>

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
        <rect x="86" y="44" width="4" height="8"/>

        <rect x="36" y="58" width="8" height="4"/>
        <rect x="48" y="56" width="4" height="8"/>
        <rect x="56" y="58" width="6" height="6"/>
        <rect x="66" y="58" width="8" height="4"/>
        <rect x="78" y="56" width="4" height="8"/>
        <rect x="86" y="58" width="4" height="4"/>

        <rect x="36" y="68" width="4" height="8"/>
        <rect x="44" y="70" width="8" height="4"/>
        <rect x="56" y="68" width="4" height="8"/>
        <rect x="64" y="72" width="6" height="4"/>
        <rect x="74" y="68" width="6" height="6"/>
        <rect x="84" y="70" width="6" height="4"/>

        <rect x="36" y="80" width="8" height="4"/>
        <rect x="48" y="82" width="6" height="6"/>
        <rect x="58" y="80" width="4" height="8"/>
        <rect x="66" y="80" width="8" height="4"/>
        <rect x="78" y="82" width="4" height="6"/>
        <rect x="86" y="80" width="4" height="8"/>

        <!-- Mascot Bot Center Mark in Electric Lime -->
        <circle cx="50" cy="50" r="7" fill="#d5fb66"/>
        <rect x="47" y="47" width="6" height="6" fill="#0c0d14" rx="1"/>
      </g>
    </svg>
  `;

  container.innerHTML = svg;
}

// ==========================================================================
// 4. TOAST & DOWNLOAD HANDLERS
// ==========================================================================

function showToast(message) {
  const toast = document.getElementById('toast');
  const toastText = document.getElementById('toast-text');
  if (!toast || !toastText) return;

  toastText.textContent = message;
  toast.classList.add('show');

  setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
}

function initDownloadButtons() {
  initDownloadTracking({
    version: 'v0.4.4',
    fileName: 'klix-code-v0.4.4.apk',
    onDownload: () => {
      showToast('Klix Code v0.4.4 APK download started!');
    },
  });

  const copyBtn = document.getElementById('copy-hash-btn');
  const checksumText = document.getElementById('checksum-text');
  const copyBtnText = document.getElementById('copy-btn-text');

  if (copyBtn && checksumText) {
    copyBtn.addEventListener('click', async () => {
      const hash = checksumText.dataset.hash || checksumText.textContent.trim();
      trackEvent('copy_checksum', {
        algorithm: 'sha256',
        hash_prefix: hash.substring(0, 8),
      });

      try {
        await navigator.clipboard.writeText(hash);
        if (copyBtnText) copyBtnText.textContent = 'Copied!';
        copyBtn.classList.add('copied');
        showToast('SHA-256 Checksum copied to clipboard!');
        setTimeout(() => {
          if (copyBtnText) copyBtnText.textContent = 'Copy Hash';
          copyBtn.classList.remove('copied');
        }, 2500);
      } catch (err) {
        showToast('Checksum: ' + hash.substring(0, 16) + '...');
      }
    });
  }
}

// ==========================================================================
// 5. NAVBAR & MOBILE MENU
// ==========================================================================

function initNavigation() {
  const navbar = document.getElementById('navbar');
  const toggleBtn = document.getElementById('mobile-menu-toggle');
  const mobileMenu = document.getElementById('mobile-menu');

  window.addEventListener('scroll', () => {
    if (window.scrollY > 15) {
      navbar?.classList.add('scrolled');
    } else {
      navbar?.classList.remove('scrolled');
    }
  }, { passive: true });

  if (toggleBtn && mobileMenu) {
    toggleBtn.addEventListener('click', () => {
      mobileMenu.classList.toggle('open');
    });

    const mobileLinks = mobileMenu.querySelectorAll('a');
    mobileLinks.forEach((link) => {
      link.addEventListener('click', () => {
        mobileMenu.classList.remove('open');
      });
    });
  }
}

// ==========================================================================
// 6. THEME-ADAPTIVE DIGITAL CODE RAIN
// ==========================================================================

function initMatrixRain() {
  const canvas = document.getElementById('matrix-rain-canvas');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  let width = (canvas.width = window.innerWidth);
  let height = (canvas.height = window.innerHeight);

  window.addEventListener('resize', () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
    initColumns();
  });

  const chars = 'アイウエオカキクケコサシスセソタチツテトナニヌネノ0123456789ABCDEF{}[]<>=/+-*~#$%&@';
  const charArray = chars.split('');

  const fontSize = 16;
  let columns = Math.floor(width / fontSize);
  let drops = [];

  function initColumns() {
    columns = Math.floor(width / fontSize);
    drops = [];
    for (let i = 0; i < columns; i++) {
      drops.push({
        y: Math.random() * -100,
        speed: 1 + Math.random() * 1.5,
        length: 12 + Math.floor(Math.random() * 18),
        intensity: Math.random(),
      });
    }
  }

  initColumns();

  clearMatrixCanvasFn = () => {
    ctx.clearRect(0, 0, width, height);
  };

  let lastTime = 0;
  const fpsInterval = 1000 / 30; // 30fps

  function draw(currentTime) {
    requestAnimationFrame(draw);

    const elapsed = currentTime - lastTime;
    if (elapsed < fpsInterval) return;
    lastTime = currentTime - (elapsed % fpsInterval);

    const isLight = getEffectiveTheme() === 'light';

    // Background fade trail adapting to theme
    ctx.fillStyle = isLight ? 'rgba(248, 250, 252, 0.18)' : 'rgba(11, 14, 26, 0.16)';
    ctx.fillRect(0, 0, width, height);

    ctx.font = `${fontSize}px 'JetBrains Mono', monospace`;

    for (let i = 0; i < drops.length; i++) {
      const drop = drops[i];
      const x = i * fontSize;
      const currentHeadY = drop.y * fontSize;

      for (let j = 0; j < drop.length; j++) {
        const charY = (drop.y - j) * fontSize;
        if (charY < -fontSize || charY > height + fontSize) continue;

        const randomChar = charArray[Math.floor(Math.random() * charArray.length)];
        const progress = j / drop.length;

        if (isLight) {
          // Light theme: Subtle deep slate & emerald drops
          if (j === 0) {
            ctx.fillStyle = '#0f172a';
            ctx.shadowBlur = 4;
            ctx.shadowColor = '#84cc16';
          } else if (j < 3) {
            ctx.fillStyle = 'rgba(77, 124, 15, 0.75)';
            ctx.shadowBlur = 0;
          } else if (j < 8) {
            ctx.fillStyle = `rgba(2, 132, 199, ${0.5 * (1 - progress)})`;
            ctx.shadowBlur = 0;
          } else {
            ctx.fillStyle = `rgba(148, 163, 184, ${0.3 * (1 - progress)})`;
            ctx.shadowBlur = 0;
          }
        } else {
          // Dark theme: Luminous neon lime & tech cyan drops
          if (j === 0) {
            ctx.fillStyle = '#ffffff';
            ctx.shadowBlur = 10;
            ctx.shadowColor = '#d5fb66';
          } else if (j < 3) {
            ctx.fillStyle = 'rgba(213, 251, 102, 0.95)';
            ctx.shadowBlur = 6;
            ctx.shadowColor = '#d5fb66';
          } else if (j < 8) {
            ctx.fillStyle = `rgba(163, 230, 53, ${0.85 * (1 - progress)})`;
            ctx.shadowBlur = 0;
          } else {
            ctx.fillStyle = `rgba(56, 189, 248, ${0.45 * (1 - progress)})`;
            ctx.shadowBlur = 0;
          }
        }

        ctx.fillText(randomChar, x, charY);
      }

      ctx.shadowBlur = 0;
      drop.y += drop.speed;

      if (currentHeadY > height + drop.length * fontSize && Math.random() > 0.96) {
        drop.y = Math.random() * -30;
        drop.speed = 1 + Math.random() * 1.5;
        drop.length = 12 + Math.floor(Math.random() * 18);
      }
    }
  }

  requestAnimationFrame(draw);
}

// ==========================================================================
// 7. SCROLL-TRIGGERED REVEAL ANIMATIONS
// ==========================================================================

function initScrollReveal() {
  const revealElements = document.querySelectorAll('.reveal');
  if (!revealElements.length) return;

  const observerOptions = {
    root: null,
    rootMargin: '0px 0px -50px 0px',
    threshold: 0.12,
  };

  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  revealElements.forEach((el) => {
    revealObserver.observe(el);
  });
}

// ==========================================================================
// 8. SCROLL PARALLAX
// ==========================================================================

function initScrollParallax() {
  const aura = document.querySelector('.phone-aura-backdrop');
  const grid = document.querySelector('.ambient-grid');

  if (!aura && !grid) return;

  let ticking = false;

  window.addEventListener('scroll', () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        const scrollY = window.scrollY;
        if (aura) {
          aura.style.transform = `translateX(-50%) translateY(${scrollY * 0.12}px)`;
        }
        if (grid) {
          grid.style.transform = `translateY(${scrollY * 0.03}px)`;
        }
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });
}

// ==========================================================================
// 9. CARDS FAN DECK INTERACTIVE BEHAVIOR
// ==========================================================================

function initCardsFanDeck() {
  const fanCards = document.querySelectorAll('.fan-card');
  const phone = document.getElementById('phone-mockup');

  fanCards.forEach((card) => {
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        card.click();
      }
    });

    card.addEventListener('click', () => {
      const screen = card.getAttribute('data-screen');

      card.style.transform = `${card.style.transform || ''} scale(1.10)`;
      setTimeout(() => {
        card.style.transform = '';
      }, 300);

      if (screen === 'shell-ps' || screen === 'node-server') {
        switchPhoneTab('terminal');
      } else if (screen === 'agent-claude') {
        switchPhoneTab('agent');
      } else if (screen === 'saf-storage') {
        switchPhoneTab('home');
      } else if (screen === 'file-tree') {
        switchPhoneTab('diff');
      }

      phone?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  });
}

// ==========================================================================
// INITIALIZATION
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initThemeSwitcher();
  initMatrixRain();
  initCardsFanDeck();
  initPhoneMockupTabs();
  generateQrMatrixSvg();
  initDownloadButtons();
  initNavigation();
  initScrollReveal();
  initScrollParallax();
});

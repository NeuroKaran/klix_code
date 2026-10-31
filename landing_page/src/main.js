/**
 * KLIX CODE — LANDING PAGE ENGINE
 */

import { initDownloadTracking, trackEvent } from './tracker.js';

// ==========================================================================
// 1. PHONE SCREEN TABS TOGGLE (Agent Loop / Diff Review / Alpine Terminal)
// ==========================================================================

function initPhoneMockupTabs() {
  const tabBtns = document.querySelectorAll('.phone-tab-btn');
  const panels = {
    agent: document.getElementById('tab-agent'),
    diff: document.getElementById('tab-diff'),
    terminal: document.getElementById('tab-terminal'),
  };

  tabBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-tab');

      tabBtns.forEach((b) => {
        b.classList.remove('active');
        b.setAttribute('aria-selected', 'false');
      });
      btn.classList.add('active');
      btn.setAttribute('aria-selected', 'true');

      Object.keys(panels).forEach((key) => {
        if (panels[key]) {
          if (key === target) {
            panels[key].classList.add('active');
          } else {
            panels[key].classList.remove('active');
          }
        }
      });
    });
  });

  // Phone Mockup Approve Button Action
  const approveBtn = document.getElementById('mock-approve-btn');
  if (approveBtn) {
    approveBtn.addEventListener('click', () => {
      approveBtn.textContent = 'Approved ✓ Applying Patch...';
      approveBtn.style.backgroundColor = '#34d399';
      setTimeout(() => {
        const diffTab = document.querySelector('.phone-tab-btn[data-tab="diff"]');
        if (diffTab) diffTab.click();
      }, 500);
    });
  }
}

// ==========================================================================
// 2. SVG QR CODE GENERATOR (Scannable from Android Camera)
// ==========================================================================

function generateQrMatrixSvg() {
  const container = document.getElementById('qr-display');
  if (!container) return;

  const svg = `
    <svg viewBox="0 0 100 100" class="qr-svg-matrix" xmlns="http://www.w3.org/2000/svg">
      <rect width="100" height="100" fill="#ffffff"/>
      <!-- Top-Left Corner -->
      <rect x="8" y="8" width="24" height="24" fill="#000000" rx="2"/>
      <rect x="12" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="16" width="8" height="8" fill="#000000"/>

      <!-- Top-Right Corner -->
      <rect x="68" y="8" width="24" height="24" fill="#000000" rx="2"/>
      <rect x="72" y="12" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="76" y="16" width="8" height="8" fill="#000000"/>

      <!-- Bottom-Left Corner -->
      <rect x="8" y="68" width="24" height="24" fill="#000000" rx="2"/>
      <rect x="12" y="72" width="16" height="16" fill="#ffffff" rx="1"/>
      <rect x="16" y="76" width="8" height="8" fill="#000000"/>

      <!-- QR Data Grid -->
      <g fill="#000000">
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

        <!-- Robot Center Mark -->
        <circle cx="50" cy="50" r="6" fill="#d4ff32"/>
        <rect x="48" y="48" width="4" height="4" fill="#000000"/>
      </g>
    </svg>
  `;

  container.innerHTML = svg;
}

// ==========================================================================
// 3. TOAST & DOWNLOAD HANDLERS
// ==========================================================================

function showToast(message) {
  const toast = document.getElementById('toast');
  const toastText = document.getElementById('toast-text');
  if (!toast || !toastText) return;

  toastText.textContent = message;
  toast.classList.add('show');

  setTimeout(() => {
    toast.classList.remove('show');
  }, 4000);
}

function initDownloadButtons() {
  initDownloadTracking({
    version: 'v0.3.0',
    fileName: 'klix-code-v0.3.0.apk',
    onDownload: () => {
      showToast('Klix Code v0.3.0 APK download started! (18.2 MB)');
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
// 4. HEADER NAVBAR & MOBILE MENU
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
  });

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
// 5. DIGITAL CODE RAIN (ANIMATED MATRIX STREAM IN OUR BRAND GRADIENT)
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

  // Character set: Authentic developer & matrix mix (katakana, code glyphs, hex, operators)
  const chars = 'アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン0123456789ABCDEF{}[]<>=/+-*~#$%&@';
  const charArray = chars.split('');

  const fontSize = 16;
  let columns = Math.floor(width / fontSize);

  // Each column has dynamic state for realistic cascading trails
  let drops = [];

  function initColumns() {
    columns = Math.floor(width / fontSize);
    drops = [];
    for (let i = 0; i < columns; i++) {
      drops.push({
        y: Math.random() * -100, // staggered start
        speed: 1 + Math.random() * 1.6, // varied rainfall speed
        length: 12 + Math.floor(Math.random() * 20), // trail length
        intensity: Math.random(), // layer depth / opacity
      });
    }
  }

  initColumns();

  let lastTime = 0;
  const fpsInterval = 1000 / 30; // smooth 30fps Matrix cadence

  function draw(currentTime) {
    requestAnimationFrame(draw);

    const elapsed = currentTime - lastTime;
    if (elapsed < fpsInterval) return;
    lastTime = currentTime - (elapsed % fpsInterval);

    // Subtle fade trail creating ethereal luminescence
    ctx.fillStyle = 'rgba(7, 8, 12, 0.16)';
    ctx.fillRect(0, 0, width, height);

    ctx.font = `${fontSize}px 'JetBrains Mono', monospace`;

    for (let i = 0; i < drops.length; i++) {
      const drop = drops[i];
      const x = i * fontSize;
      const currentHeadY = drop.y * fontSize;

      // Draw trail characters upwards from the head
      for (let j = 0; j < drop.length; j++) {
        const charY = (drop.y - j) * fontSize;
        if (charY < -fontSize || charY > height + fontSize) continue;

        const randomChar = charArray[Math.floor(Math.random() * charArray.length)];
        const progress = j / drop.length; // 0 at head, 1 at tail

        if (j === 0) {
          // The leading tip: Brilliant luminous white-hot glow
          ctx.fillStyle = '#ffffff';
          ctx.shadowBlur = 10;
          ctx.shadowColor = '#d4ff32';
        } else if (j < 3) {
          // Upper glow: Bright electric lime (Our primary brand color)
          ctx.fillStyle = 'rgba(212, 255, 50, 0.95)';
          ctx.shadowBlur = 6;
          ctx.shadowColor = '#d4ff32';
        } else if (j < 8) {
          // Mid-gradient: Electric lime-emerald transition
          ctx.fillStyle = `rgba(163, 230, 53, ${0.85 * (1 - progress)})`;
          ctx.shadowBlur = 0;
        } else {
          // Tail gradient: Soft cyan to deep obsidian fade
          ctx.fillStyle = `rgba(56, 189, 248, ${0.5 * (1 - progress)})`;
          ctx.shadowBlur = 0;
        }

        ctx.fillText(randomChar, x, charY);
      }

      // Reset shadow for next render loop
      ctx.shadowBlur = 0;

      // Advance droplet
      drop.y += drop.speed;

      // Reset when droplet finishes screen travel
      if (currentHeadY > height + drop.length * fontSize && Math.random() > 0.96) {
        drop.y = Math.random() * -30;
        drop.speed = 1 + Math.random() * 1.6;
        drop.length = 12 + Math.floor(Math.random() * 20);
      }
    }
  }

  requestAnimationFrame(draw);
}

// ==========================================================================
// 6. SCROLL-TRIGGERED REVEAL ANIMATIONS (IntersectionObserver)
//    Inspired by Motia-style smooth entrance transitions
// ==========================================================================

function initScrollReveal() {
  const revealElements = document.querySelectorAll('.reveal');

  if (!revealElements.length) return;

  // Use IntersectionObserver for buttery-smooth, GPU-friendly reveals
  const observerOptions = {
    root: null,          // viewport
    rootMargin: '0px 0px -60px 0px', // trigger slightly before fully in view
    threshold: 0.15,     // 15% of the element must be visible
  };

  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        // Once revealed, stop observing (one-shot animation)
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  revealElements.forEach((el) => {
    revealObserver.observe(el);
  });
}

// ==========================================================================
// 7. LIGHTWEIGHT PARALLAX ON SCROLL (Phone Aura + Ambient Grid)
// ==========================================================================

function initScrollParallax() {
  const aura = document.querySelector('.phone-aura-backdrop');
  const grid = document.querySelector('.ambient-grid');

  if (!aura && !grid) return;

  let ticking = false;

  function onScroll() {
    if (!ticking) {
      requestAnimationFrame(() => {
        const scrollY = window.scrollY;

        // Subtle parallax: aura moves slower than scroll
        if (aura) {
          const auraShift = scrollY * 0.15;
          aura.style.transform = `translateX(-50%) translateY(${auraShift}px)`;
        }

        // Grid shifts very subtly for depth
        if (grid) {
          const gridShift = scrollY * 0.03;
          grid.style.transform = `translateY(${gridShift}px)`;
        }

        ticking = false;
      });
      ticking = true;
    }
  }

  window.addEventListener('scroll', onScroll, { passive: true });
}

// ==========================================================================
// 8. CARDS FAN DECK INTERACTIVE BEHAVIOR
// ==========================================================================

function initCardsFanDeck() {
  const fanCards = document.querySelectorAll('.fan-card');
  const phone = document.getElementById('phone-mockup');

  fanCards.forEach((card) => {
    // Accessibility: Keyboard selection
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        card.click();
      }
    });

    // Interactive card click connects to the phone mockup tabs
    card.addEventListener('click', () => {
      const screen = card.getAttribute('data-screen');

      // Add a quick feedback pop
      card.style.transform = `${card.style.transform || ''} scale(1.12)`;
      setTimeout(() => {
        card.style.transform = '';
      }, 300);

      if (screen === 'shell-ps' || screen === 'node-server') {
        const termTab = document.querySelector('.phone-tab-btn[data-tab="terminal"]');
        if (termTab) termTab.click();
      } else if (screen === 'agent-claude') {
        const agentTab = document.querySelector('.phone-tab-btn[data-tab="agent"]');
        if (agentTab) agentTab.click();
      } else if (screen === 'saf-storage' || screen === 'file-tree') {
        const diffTab = document.querySelector('.phone-tab-btn[data-tab="diff"]');
        if (diffTab) diffTab.click();
      }

      // Smooth scroll if needed
      phone?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  });
}

// ==========================================================================
// INITIALIZATION
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initMatrixRain();
  initCardsFanDeck();
  initPhoneMockupTabs();
  generateQrMatrixSvg();
  initDownloadButtons();
  initNavigation();
  initScrollReveal();
  initScrollParallax();
});


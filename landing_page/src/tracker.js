/**
 * Klix Code — Umami Analytics Tracker Utility
 * (Mirrored from /analytics/tracker.js for self-contained landing page build)
 */

const IS_DEV =
  typeof window !== 'undefined' &&
  (window.location.hostname === 'localhost' ||
    window.location.hostname === '127.0.0.1');

function devLog(action, payload) {
  if (IS_DEV) {
    console.log(
      `%c[Umami Analytics]%c ${action}`,
      'color: #a3e635; font-weight: bold; background: #12141a; padding: 2px 6px; border-radius: 4px;',
      'color: #94a3b8;',
      payload || ''
    );
  }
}

export function trackEvent(eventName, eventData = {}) {
  try {
    if (typeof window !== 'undefined' && window.umami && typeof window.umami.track === 'function') {
      window.umami.track(eventName, eventData);
      devLog(`Event sent: "${eventName}"`, eventData);
    } else {
      devLog(`Mocked event: "${eventName}" (Umami not loaded or local dev)`, eventData);
    }
  } catch (err) {
    console.warn('[Umami Analytics] Error dispatching event:', err);
  }
}

export function trackDownload({
  version = 'v0.3.0',
  source = 'unknown',
  fileName = 'klix-code-v0.3.0.apk',
} = {}) {
  trackEvent('apk_download', {
    version,
    source,
    file_name: fileName,
  });
}

export function getDownloadSource(element) {
  if (element.id === 'hero-download-btn' || element.closest('#hero')) {
    return 'hero';
  }
  if (element.id === 'main-download-button' || element.closest('#download')) {
    return 'main_card';
  }
  if (element.classList.contains('mobile-download-btn') || element.closest('#mobile-menu')) {
    return 'mobile_nav';
  }
  if (element.classList.contains('footer-apk-link') || element.closest('footer')) {
    return 'footer';
  }
  return 'unknown';
}

export function initDownloadTracking({
  version = 'v0.3.0',
  fileName = 'klix-code-v0.3.0.apk',
  onDownload = null,
} = {}) {
  if (typeof document === 'undefined') return;

  const downloadLinks = document.querySelectorAll('a[download], a[href*=".apk"]');

  downloadLinks.forEach((link) => {
    if (link.dataset.analyticsBound === 'true') return;
    link.dataset.analyticsBound = 'true';

    link.addEventListener('click', () => {
      const source = getDownloadSource(link);
      const downloadVersion = link.dataset.version || version;
      const downloadFile = link.getAttribute('download') || fileName;

      trackDownload({
        version: downloadVersion,
        source,
        fileName: downloadFile,
      });

      if (typeof onDownload === 'function') {
        onDownload({ version: downloadVersion, source, fileName: downloadFile });
      }
    });
  });

  devLog(`Bound download tracking to ${downloadLinks.length} links on page`);
}

/**
 * Klix Code — Umami Analytics Tracker Utility
 *
 * Provides safe event dispatching for Umami Analytics, with:
 * - Safe fallback if script is blocked by AdBlock
 * - Automatic metadata enrichment (version, source, timestamp)
 * - Development console logging for quick debugging
 * - Auto-binding to APK download links
 */

const IS_DEV =
  typeof window !== 'undefined' &&
  (window.location.hostname === 'localhost' ||
    window.location.hostname === '127.0.0.1');

/**
 * Log analytics event to console during local development
 */
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

/**
 * Safely send custom event to Umami
 * @param {string} eventName - Name of the custom event (e.g., 'apk_download')
 * @param {Record<string, any>} eventData - Custom key-value properties
 */
export function trackEvent(eventName, eventData = {}) {
  try {
    if (typeof window !== 'undefined' && window.umami && typeof window.umami.track === 'function') {
      window.umami.track(eventName, eventData);
      devLog(`Event sent: "${eventName}"`, eventData);
    } else {
      devLog(`Queued/Mocked event: "${eventName}" (Umami not loaded or local dev)`, eventData);
    }
  } catch (err) {
    console.warn('[Umami Analytics] Error dispatching event:', err);
  }
}

/**
 * Track an APK download event with structured metadata
 * @param {Object} options
 * @param {string} [options.version='v0.3.0']
 * @param {'hero' | 'main_card' | 'mobile_nav' | 'footer' | 'qr_scan' | 'unknown'} [options.source='unknown']
 * @param {string} [options.fileName='klix-code-v0.3.0.apk']
 */
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

/**
 * Determine the visual source of a download button
 * @param {HTMLElement} element
 * @returns {'hero' | 'main_card' | 'mobile_nav' | 'footer' | 'unknown'}
 */
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

/**
 * Automatically bind tracking to all APK download links on the page
 * @param {Object} [config]
 * @param {string} [config.version='v0.3.0']
 * @param {string} [config.fileName='klix-code-v0.3.0.apk']
 * @param {Function} [config.onDownload] - Optional callback triggered on click (e.g. show toast)
 */
export function initDownloadTracking({
  version = 'v0.3.0',
  fileName = 'klix-code-v0.3.0.apk',
  onDownload = null,
} = {}) {
  if (typeof document === 'undefined') return;

  const downloadLinks = document.querySelectorAll('a[download], a[href*=".apk"]');

  downloadLinks.forEach((link) => {
    // Avoid double attaching
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

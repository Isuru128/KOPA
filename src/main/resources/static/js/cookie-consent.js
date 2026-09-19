/**
 * KOPA Specialty Coffee — Cookie & Privacy Consent Handler
 */

export const CookieConsent = {
  popupEl: null,

  init() {
    this.popupEl = document.getElementById('cookieConsentPopup');
    if (!this.popupEl) return;

    this.bindEvents();
    this.checkConsent();
  },

  checkConsent() {
    const consent = localStorage.getItem('kopa_cookie_consent');
    if (!consent) {
      // Show smooth pop-up after a brief welcoming pause
      setTimeout(() => {
        this.show();
      }, 700);
    }
  },

  show() {
    if (this.popupEl) {
      this.popupEl.classList.add('show');
      this.popupEl.setAttribute('aria-hidden', 'false');
    }
  },

  hide() {
    if (this.popupEl) {
      this.popupEl.classList.remove('show');
      this.popupEl.setAttribute('aria-hidden', 'true');
    }
  },

  bindEvents() {
    const acceptAllBtn = document.getElementById('cookieAcceptAllBtn');
    const essentialBtn = document.getElementById('cookieEssentialBtn');
    const dismissBtn = document.getElementById('cookieDismissBtn');
    const togglePrefBtn = document.getElementById('cookieTogglePrefBtn');
    const prefSection = document.getElementById('cookiePreferencesSection');

    if (acceptAllBtn) {
      acceptAllBtn.addEventListener('click', () => {
        localStorage.setItem('kopa_cookie_consent', 'all');
        localStorage.setItem('kopa_cookie_analytics', 'true');
        localStorage.setItem('kopa_cookie_marketing', 'true');
        this.hide();
      });
    }

    if (essentialBtn) {
      essentialBtn.addEventListener('click', () => {
        localStorage.setItem('kopa_cookie_consent', 'essential');
        localStorage.setItem('kopa_cookie_analytics', 'false');
        localStorage.setItem('kopa_cookie_marketing', 'false');
        this.hide();
      });
    }

    if (dismissBtn) {
      dismissBtn.addEventListener('click', () => {
        this.hide();
      });
    }

    if (togglePrefBtn && prefSection) {
      togglePrefBtn.addEventListener('click', () => {
        const isHidden = prefSection.style.display === 'none';
        prefSection.style.display = isHidden ? 'flex' : 'none';
        togglePrefBtn.innerText = isHidden ? 'Hide Preferences ▲' : 'Customize Preferences ▼';
      });
    }

    // Global trigger for footer links
    document.querySelectorAll('[data-action="open-cookie-preferences"]').forEach(el => {
      el.addEventListener('click', (e) => {
        e.preventDefault();
        if (prefSection) {
          prefSection.style.display = 'flex';
          if (togglePrefBtn) togglePrefBtn.innerText = 'Hide Preferences ▲';
        }
        this.show();
      });
    });
  }
};

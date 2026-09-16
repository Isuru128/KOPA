/**
 * KOPA Coffee — Authentication & User Profile Module
 */

import { ApiClient } from './api.js';
import { Cart } from './cart.js';

export const Auth = {
  currentUser: null,

  init() {
    this.loadSession();
    this.bindEvents();
    this.updateUI();
  },

  loadSession() {
    const saved = localStorage.getItem('kopa_auth_user') || localStorage.getItem('kopa_user');
    if (saved) {
      try {
        this.currentUser = JSON.parse(saved);
        localStorage.setItem('kopa_auth_user', JSON.stringify(this.currentUser));
      } catch (e) {
        this.currentUser = null;
      }
    }
  },

  getCurrentUser() {
    return this.currentUser;
  },

  saveGuestSession(data) {
    this.currentUser = {
      id: 'usr-guest-' + Date.now(),
      name: data.name,
      email: data.email,
      phone: data.phone,
      role: 'GUEST'
    };
    localStorage.setItem('kopa_auth_user', JSON.stringify(this.currentUser));
    this.updateUI();
  },

  async login(email, password) {
    try {
      const user = await ApiClient.login(email, password);
      this.currentUser = user;
      localStorage.setItem('kopa_auth_user', JSON.stringify(user));
      this.updateUI();
      Cart.showToast(`Welcome back, ${user.name}`);
      this.closeModal();
      if (window.loadMyReservations) window.loadMyReservations();
      return user;
    } catch (err) {
      Cart.showToast(err.message || 'Login failed');
      throw err;
    }
  },

  async register(name, email, phone, password) {
    try {
      const user = await ApiClient.register(name, email, phone, password);
      this.currentUser = user;
      localStorage.setItem('kopa_auth_user', JSON.stringify(user));
      this.updateUI();
      Cart.showToast(`Welcome to KOPA, ${user.name}`);
      this.closeModal();
      if (window.loadMyReservations) window.loadMyReservations();
      return user;
    } catch (err) {
      Cart.showToast(err.message || 'Registration failed');
      throw err;
    }
  },

  logout() {
    this.currentUser = null;
    localStorage.removeItem('kopa_auth_user');
    this.updateUI();
    Cart.showToast('Logged out successfully');
    if (window.loadMyReservations) window.loadMyReservations();
  },

  updateUI() {
    const userBtn = document.getElementById('navUserBtn');
    const userDropdown = document.getElementById('userProfileDropdown');
    const userNameSpan = document.getElementById('dropdownUserName');
    const userEmailSpan = document.getElementById('dropdownUserEmail');

    if (!userBtn) return;

    if (this.currentUser) {
      userBtn.innerHTML = `
        <span style="display:flex;align-items:center;gap:6px;">
          <span style="width:26px;height:26px;border-radius:50%;background:var(--accent-amber);color:var(--bg-primary);font-size:0.75rem;font-weight:700;display:flex;align-items:center;justify-content:center;">
            ${(this.currentUser.name || 'U')[0].toUpperCase()}
          </span>
          <span style="font-size:0.9rem;font-weight:600;">${this.currentUser.name.split(' ')[0]}</span>
        </span>
      `;
      if (userNameSpan) userNameSpan.textContent = this.currentUser.name;
      if (userEmailSpan) userEmailSpan.textContent = this.currentUser.email;
    } else {
      userBtn.innerHTML = `
        <span style="font-size:0.9rem;font-weight:600;">Sign In</span>
      `;
    }
  },

  bindEvents() {
    const modal = document.getElementById('authModal');
    const openBtns = document.querySelectorAll('[data-action="open-auth"]');
    const closeBtn = document.getElementById('closeAuthModalBtn');
    const loginTabBtn = document.getElementById('authLoginTabBtn');
    const registerTabBtn = document.getElementById('authRegisterTabBtn');
    const loginForm = document.getElementById('authLoginForm');
    const registerForm = document.getElementById('authRegisterForm');
    const demoLoginBtn = document.getElementById('authDemoLoginBtn');
    const logoutBtn = document.getElementById('userLogoutBtn');

    openBtns.forEach(btn => {
      btn.onclick = () => {
        if (this.currentUser) {
          // If logged in, navigate to My Reservations
          const dash = document.getElementById('myReservations');
          if (dash) dash.scrollIntoView({ behavior: 'smooth' });
        } else {
          this.openModal();
        }
      };
    });

    if (closeBtn) closeBtn.onclick = () => this.closeModal();
    if (modal) {
      modal.onclick = (e) => {
        if (e.target === modal) this.closeModal();
      };
    }

    // Tabs
    if (loginTabBtn && registerTabBtn) {
      loginTabBtn.onclick = () => {
        loginTabBtn.classList.add('active');
        registerTabBtn.classList.remove('active');
        if (loginForm) loginForm.style.display = 'block';
        if (registerForm) registerForm.style.display = 'none';
      };
      registerTabBtn.onclick = () => {
        registerTabBtn.classList.add('active');
        loginTabBtn.classList.remove('active');
        if (loginForm) loginForm.style.display = 'none';
        if (registerForm) registerForm.style.display = 'block';
      };
    }

    // Login submit
    if (loginForm) {
      loginForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmailInput').value;
        const pass = document.getElementById('loginPasswordInput').value;
        await this.login(email, pass);
      };
    }

    // Register submit
    if (registerForm) {
      registerForm.onsubmit = async (e) => {
        e.preventDefault();
        const name = document.getElementById('regNameInput').value;
        const email = document.getElementById('regEmailInput').value;
        const phone = document.getElementById('regPhoneInput').value;
        const pass = document.getElementById('regPasswordInput').value;
        await this.register(name, email, phone, pass);
      };
    }

    // 1-Click Demo Login
    if (demoLoginBtn) {
      demoLoginBtn.onclick = async () => {
        await this.login('guest@kopa.coffee', 'kopa123');
      };
    }

    // Logout
    if (logoutBtn) {
      logoutBtn.onclick = () => this.logout();
    }
  },

  openModal() {
    const modal = document.getElementById('authModal');
    if (modal) modal.classList.add('active');
  },

  closeModal() {
    const modal = document.getElementById('authModal');
    if (modal) modal.classList.remove('active');
  }
};

/**
 * KOPA Specialty Coffee Shop — Main Application Orchestrator
 */

import { Cart } from './cart.js';
import { Menu } from './menu.js';
import { Reservation } from './reservation.js';
import { Auth } from './auth.js';
import { Dashboard } from './dashboard.js';
import { CookieConsent } from './cookie-consent.js';

document.addEventListener('DOMContentLoaded', async () => {
  // Initialize Core Systems
  Cart.init();
  Auth.init();
  CookieConsent.init();
  await Menu.init();
  Reservation.init();
  await Dashboard.init();

  // Setup Mobile Hamburger Menu
  const hamburgerBtn = document.getElementById('hamburgerBtn');
  const mobileNav = document.getElementById('mobileNavDrawer');
  const closeMobileNav = document.getElementById('closeMobileNavBtn');
  const mobileNavLinks = document.querySelectorAll('.mobile-nav-link');

  if (hamburgerBtn && mobileNav) {
    hamburgerBtn.onclick = () => mobileNav.classList.add('open');
  }
  if (closeMobileNav && mobileNav) {
    closeMobileNav.onclick = () => mobileNav.classList.remove('open');
  }
  mobileNavLinks.forEach(link => {
    link.onclick = () => {
      if (mobileNav) mobileNav.classList.remove('open');
    };
  });

  // Highlight active nav item on scroll
  const sections = document.querySelectorAll('section[id]');
  const navLinks = document.querySelectorAll('.nav-link');

  window.addEventListener('scroll', () => {
    let current = '';
    sections.forEach(section => {
      const top = section.offsetTop - 120;
      const height = section.offsetHeight;
      if (window.scrollY >= top && window.scrollY < top + height) {
        current = section.getAttribute('id');
      }
    });

    navLinks.forEach(link => {
      link.classList.remove('active');
      if (link.getAttribute('href') === `#${current}`) {
        link.classList.add('active');
      }
    });
  });

  // Global CTA bindings
  document.querySelectorAll('[data-cta="reserve-now"]').forEach(btn => {
    btn.onclick = () => {
      const resSec = document.getElementById('reserve');
      if (resSec) resSec.scrollIntoView({ behavior: 'smooth' });
    };
  });

  document.querySelectorAll('[data-cta="explore-menu"]').forEach(btn => {
    btn.onclick = () => {
      const menuSec = document.getElementById('menu');
      if (menuSec) menuSec.scrollIntoView({ behavior: 'smooth' });
    };
  });
});

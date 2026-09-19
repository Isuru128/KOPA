/**
 * KOPA Coffee — Customer Dashboard ("My Reservations")
 */

import { ApiClient } from './api.js';
import { Auth } from './auth.js';
import { Cart } from './cart.js';
import { Reservation } from './reservation.js';

export const Dashboard = {
  reservations: [],

  async init() {
    window.loadMyReservations = () => this.loadReservations();
    await this.loadReservations();
    this.bindEvents();
  },

  async loadReservations() {
    const grid = document.getElementById('myReservationsGrid');
    if (!grid) return;

    const currentUser = Auth.getCurrentUser();
    if (!currentUser || !currentUser.email) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; background: var(--bg-surface); border: 1px dashed var(--border-card); border-radius: var(--radius-lg);">
          <div style="font-size: 2.5rem; margin-bottom: 12px; color: var(--accent-caramel);">☕</div>
          <h3 style="margin-bottom: 8px; font-family: var(--font-serif); font-size: 1.4rem;">Sign in to view your reservations</h3>
          <p style="color: var(--text-secondary); max-width: 440px; margin: 0 auto 20px;">
            Access all your confirmed table bookings, live QR passes, and specialty pre-orders.
          </p>
          <div style="display: flex; gap: 12px; justify-content: center;">
            <a href="/signin" class="btn btn-primary btn-sm">Sign In</a>
            <a href="/signup" class="btn btn-outline btn-sm">Create Account</a>
          </div>
        </div>
      `;
      return;
    }

    grid.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-muted);">Loading your KOPA moments...</div>';

    try {
      this.reservations = await ApiClient.getUserReservations(currentUser.email);
      this.render();
    } catch (e) {
      grid.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; color: var(--text-muted);">Failed to load reservations.</div>';
    }
  },

  render() {
    const grid = document.getElementById('myReservationsGrid');
    if (!grid) return;

    if (this.reservations.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; background: var(--bg-surface); border: 1px dashed var(--border-card); border-radius: var(--radius-lg);">
          <div style="font-size: 2.5rem; margin-bottom: 12px; color: var(--accent-caramel);">☕</div>
          <h3 style="margin-bottom: 8px; font-family: var(--font-serif); font-size: 1.4rem;">No reservations yet</h3>
          <p style="color: var(--text-secondary); max-width: 440px; margin: 0 auto 20px;">
            Reserve your favorite table and pre-order specialty coffee and pastries before you arrive.
          </p>
          <a href="#reserve" class="btn btn-primary btn-sm">Reserve a Table Now</a>
        </div>
      `;
      return;
    }

    grid.innerHTML = this.reservations.map(res => {
      const statusClass = (res.status || 'CONFIRMED').toLowerCase();
      const orderCount = (res.orderItems || []).reduce((acc, i) => acc + (i.quantity || 1), 0);
      const totalAmt = res.totalAmount || (res.orderItems || []).reduce((acc, i) => acc + (i.subtotal || 0), 0);

      return `
        <div class="res-dashboard-card" data-res-id="${res.reservationId || res.id}">
          <div class="res-card-top">
            <div>
              <span class="eyebrow" style="margin-bottom: 4px;">Reservation</span>
              <div class="res-code-label">${res.reservationId || res.id}</div>
            </div>
            <span class="status-badge ${statusClass}">${res.status || 'CONFIRMED'}</span>
          </div>

          <div class="res-details-list">
            <div class="res-detail-row">
              <span style="color: var(--accent-amber);">📅</span>
              <span><strong>${res.date}</strong> at <strong>${res.startTime}</strong> (${res.durationMinutes || 90}m)</span>
            </div>
            <div class="res-detail-row">
              <span style="color: var(--accent-amber);">🪑</span>
              <span>Table ${res.tableNumber} — ${res.tableType} (${res.tableLocation || 'Main Hall'})</span>
            </div>
            <div class="res-detail-row">
              <span style="color: var(--accent-amber);">👥</span>
              <span>${res.guestCount} Guests</span>
            </div>
            <div class="res-detail-row">
              <span style="color: var(--accent-amber);">☕</span>
              <span>${orderCount > 0 ? `${orderCount} pre-ordered items ($${totalAmt.toFixed(2)})` : 'No pre-ordered items'}</span>
            </div>
          </div>

          <div class="res-card-actions">
            <button class="btn btn-outline btn-sm" data-action="view-ticket" data-id="${res.reservationId || res.id}">
              View QR Ticket
            </button>
            ${res.status !== 'CANCELLED' ? `
              <button class="btn btn-secondary btn-sm" data-action="cancel-res" data-id="${res.reservationId || res.id}" style="color: var(--status-maintenance);">
                Cancel
              </button>
            ` : ''}
          </div>
        </div>
      `;
    }).join('');

    // Click handler for card actions
    grid.onclick = async (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      const action = btn.dataset.action;
      const resId = btn.dataset.id;
      const res = this.reservations.find(r => (r.reservationId || r.id) === resId);
      if (!res) return;

      if (action === 'view-ticket') {
        this.openTicketModal(res);
      } else if (action === 'cancel-res') {
        if (confirm(`Are you sure you want to cancel reservation ${res.reservationId || res.id}?`)) {
          btn.disabled = true;
          btn.textContent = 'Cancelling...';
          await ApiClient.cancelReservation(res.reservationId || res.id);
          Cart.showToast(`Reservation ${res.reservationId || res.id} cancelled`);
          await this.loadReservations();
        }
      }
    };
  },

  openTicketModal(res) {
    const modal = document.getElementById('ticketModal');
    const codeEl = document.getElementById('ticketModalCode');
    const bodyEl = document.getElementById('ticketModalBody');
    const qrEl = document.getElementById('ticketModalQr');
    if (!modal) return;

    if (codeEl) codeEl.textContent = res.reservationId || res.id;
    if (bodyEl) {
      bodyEl.innerHTML = `
        <div style="margin-bottom: 12px;"><strong>Guest:</strong> ${res.customerName}</div>
        <div style="margin-bottom: 12px;"><strong>When:</strong> ${res.date} at ${res.startTime}</div>
        <div style="margin-bottom: 12px;"><strong>Table:</strong> Table ${res.tableNumber} (${res.tableType})</div>
        <div style="margin-bottom: 12px;"><strong>Guests:</strong> ${res.guestCount}</div>
        <div style="margin-bottom: 12px;"><strong>Status:</strong> <span class="status-badge ${(res.status || 'confirmed').toLowerCase()}">${res.status}</span></div>
        <div style="margin-top: 16px; border-top: 1px dashed var(--border-card); padding-top: 12px;">
          <strong>Pre-ordered items:</strong>
          <div style="margin-top: 6px; font-size: 0.88rem; color: var(--text-secondary);">
            ${(res.orderItems && res.orderItems.length) ? res.orderItems.map(i => `<div>${i.quantity}× ${i.productName}</div>`).join('') : 'None'}
          </div>
        </div>
      `;
    }
    if (qrEl) {
      qrEl.innerHTML = Reservation.generateQrSvg(res.reservationId, res.customerName, res.date, res.startTime);
    }

    modal.classList.add('active');
  },

  bindEvents() {
    const ticketModal = document.getElementById('ticketModal');
    const closeBtn = document.getElementById('closeTicketModalBtn');
    if (closeBtn) closeBtn.onclick = () => ticketModal.classList.remove('active');
    if (ticketModal) {
      ticketModal.onclick = (e) => {
        if (e.target === ticketModal) ticketModal.classList.remove('active');
      };
    }
  }
};

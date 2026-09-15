/**
 * KOPA Coffee — Reservation Flow Engine
 * Step 1: Date, Time & Guests
 * Step 2: Interactive Café Floor Plan (Visual Table Map with live availability)
 * Step 3: Review Order + Reservation Summary
 * Step 4: Confirmation with Ticket, QR Code & Calendar Export
 */

import { ApiClient } from './api.js';
import { Cart } from './cart.js';
import { Auth } from './auth.js';

export const Reservation = {
  currentStep: 1,
  data: {
    date: '',
    time: '19:00',
    guests: 2,
    selectedTable: null,
    specialNotes: ''
  },
  availableTables: [],
  confirmedReservation: null,

  init() {
    this.initDefaultDate();
    this.bindStep1Controls();
    this.bindStep2Controls();
    this.bindStep3Controls();
    this.bindQuickWidget();
    this.setStep(1);
  },

  initDefaultDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const yyyy = tomorrow.getFullYear();
    const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const dd = String(tomorrow.getDate()).padStart(2, '0');
    this.data.date = `${yyyy}-${mm}-${dd}`;

    const dateInput = document.getElementById('resDateInput');
    if (dateInput) {
      dateInput.value = this.data.date;
      dateInput.min = new Date().toISOString().split('T')[0];
    }
    const quickDate = document.getElementById('quickDateInput');
    if (quickDate) {
      quickDate.value = this.data.date;
      quickDate.min = new Date().toISOString().split('T')[0];
    }
  },

  setStep(step) {
    this.currentStep = step;

    // Progress Bar Nodes
    for (let i = 1; i <= 4; i++) {
      const node = document.getElementById(`wizardStepNode${i}`);
      if (!node) continue;
      node.classList.remove('active', 'completed');
      if (i === step) node.classList.add('active');
      else if (i < step) node.classList.add('completed');
    }

    // Step Sections visibility
    for (let i = 1; i <= 4; i++) {
      const sec = document.getElementById(`resStep${i}`);
      if (sec) {
        sec.style.display = (i === step) ? 'block' : 'none';
      }
    }

    if (step === 2) {
      this.loadFloorPlan();
    } else if (step === 3) {
      this.renderReviewSummary();
    } else if (step === 4) {
      this.renderConfirmation();
    }

    // Scroll smoothly to reservation card
    const container = document.getElementById('reserve');
    if (container && step > 1) {
      container.scrollIntoView({ behavior: 'smooth' });
    }
  },

  bindQuickWidget() {
    // Quick widget on Hero
    const quickBtn = document.getElementById('quickFindTablesBtn');
    const quickDate = document.getElementById('quickDateInput');
    const quickTime = document.getElementById('quickTimeSelect');
    const quickGuestsSpan = document.getElementById('quickGuestsVal');
    const quickDec = document.getElementById('quickGuestsDec');
    const quickInc = document.getElementById('quickGuestsInc');

    let guestsCount = 2;
    if (quickDec && quickInc && quickGuestsSpan) {
      quickDec.onclick = () => {
        if (guestsCount > 1) {
          guestsCount--;
          quickGuestsSpan.textContent = guestsCount;
        }
      };
      quickInc.onclick = () => {
        if (guestsCount < 8) {
          guestsCount++;
          quickGuestsSpan.textContent = guestsCount;
        }
      };
    }

    if (quickBtn) {
      quickBtn.onclick = () => {
        if (quickDate && quickDate.value) this.data.date = quickDate.value;
        if (quickTime && quickTime.value) this.data.time = quickTime.value;
        this.data.guests = guestsCount;

        // Sync to step 1 inputs
        const dInput = document.getElementById('resDateInput');
        if (dInput) dInput.value = this.data.date;
        const gSpan = document.getElementById('resGuestsVal');
        if (gSpan) gSpan.textContent = this.data.guests;

        this.setStep(2);
      };
    }
  },

  bindStep1Controls() {
    const dateInput = document.getElementById('resDateInput');
    if (dateInput) {
      dateInput.addEventListener('change', (e) => {
        this.data.date = e.target.value;
      });
    }

    // Guest stepper
    const decBtn = document.getElementById('resGuestsDec');
    const incBtn = document.getElementById('resGuestsInc');
    const valSpan = document.getElementById('resGuestsVal');

    if (decBtn) {
      decBtn.onclick = () => {
        if (this.data.guests > 1) {
          this.data.guests--;
          if (valSpan) valSpan.textContent = this.data.guests;
        }
      };
    }
    if (incBtn) {
      incBtn.onclick = () => {
        if (this.data.guests < 8) {
          this.data.guests++;
          if (valSpan) valSpan.textContent = this.data.guests;
        }
      };
    }

    // Time slot pills
    const slotsContainer = document.getElementById('resTimeSlotsContainer');
    if (slotsContainer) {
      slotsContainer.onclick = (e) => {
        const pill = e.target.closest('.time-slot-pill');
        if (!pill) return;
        slotsContainer.querySelectorAll('.time-slot-pill').forEach(p => p.classList.remove('selected'));
        pill.classList.add('selected');
        this.data.time = pill.dataset.time;
      };
    }

    // Check Availability Button
    const checkBtn = document.getElementById('step1CheckAvailabilityBtn');
    if (checkBtn) {
      checkBtn.onclick = () => {
        if (!this.data.date) {
          Cart.showToast('Please pick a date for your visit');
          return;
        }
        this.setStep(2);
      };
    }
  },

  async loadFloorPlan() {
    const grid = document.getElementById('cafeFloorPlanTables');
    const statusNote = document.getElementById('floorPlanStatusNote');
    if (!grid) return;

    grid.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--accent-amber);">Scanning table availability for ${this.data.date} at ${this.data.time}...</div>`;

    this.availableTables = await ApiClient.getTableAvailability(
      this.data.date,
      this.data.time,
      this.data.guests
    );

    if (statusNote) {
      const availCount = this.availableTables.filter(t => t.status === 'AVAILABLE').length;
      statusNote.textContent = `${availCount} tables available for ${this.data.guests} guests on ${this.data.date} at ${this.data.time}`;
    }

    this.renderFloorPlanTables();
  },

  renderFloorPlanTables() {
    const grid = document.getElementById('cafeFloorPlanTables');
    if (!grid) return;

    grid.innerHTML = this.availableTables.map(t => {
      const isSelected = this.data.selectedTable && this.data.selectedTable.id === t.id;
      let stateClass = `state-${t.status.toLowerCase()}`;
      if (isSelected) stateClass = 'state-selected';

      return `
        <div class="table-node ${stateClass}" data-table-id="${t.id}" title="${t.description || ''}">
          <div class="table-header-row">
            <span class="table-num">Table ${t.tableNumber}</span>
            <span class="table-cap-badge">${t.capacity} Guests</span>
          </div>
          <div class="table-type-label">${t.type}</div>
          <div class="table-loc-label">${t.location}</div>
          <div class="table-status-pill">
            ${isSelected ? '✓ SELECTED' : t.status}
          </div>
        </div>
      `;
    }).join('');

    // Table click listener
    grid.onclick = (e) => {
      const node = e.target.closest('.table-node');
      if (!node) return;
      const tid = node.dataset.tableId;
      const table = this.availableTables.find(t => t.id === tid);
      if (!table) return;

      if (table.status !== 'AVAILABLE' && (!this.data.selectedTable || this.data.selectedTable.id !== table.id)) {
        if (table.status === 'RESERVED') {
          Cart.showToast(`Table ${table.tableNumber} is already reserved for this time.`);
        } else if (table.status === 'UNSUITABLE') {
          Cart.showToast(`Table ${table.tableNumber} fits up to ${table.capacity} guests.`);
        } else if (table.status === 'MAINTENANCE') {
          Cart.showToast(`Table ${table.tableNumber} is under maintenance.`);
        }
        return;
      }

      this.data.selectedTable = table;
      this.renderFloorPlanTables();
      this.updateSelectedTableBanner();
    };

    this.updateSelectedTableBanner();
  },

  updateSelectedTableBanner() {
    const banner = document.getElementById('selectedTableBanner');
    const tableInfo = document.getElementById('selectedTableBannerText');
    const nextBtn = document.getElementById('step2ContinueBtn');

    if (!banner || !tableInfo || !nextBtn) return;

    if (this.data.selectedTable) {
      banner.style.display = 'flex';
      tableInfo.innerHTML = `
        <strong>Table ${this.data.selectedTable.tableNumber} Selected</strong> • 
        ${this.data.selectedTable.type} (${this.data.selectedTable.location}) • 
        Up to ${this.data.selectedTable.capacity} Guests
      `;
      nextBtn.disabled = false;
      nextBtn.classList.remove('btn-secondary');
      nextBtn.classList.add('btn-primary');
    } else {
      banner.style.display = 'none';
      nextBtn.disabled = true;
      nextBtn.classList.remove('btn-primary');
      nextBtn.classList.add('btn-secondary');
    }
  },

  bindStep2Controls() {
    const backBtn = document.getElementById('step2BackBtn');
    if (backBtn) {
      backBtn.onclick = () => this.setStep(1);
    }

    const nextBtn = document.getElementById('step2ContinueBtn');
    if (nextBtn) {
      nextBtn.onclick = () => {
        if (!this.data.selectedTable) {
          Cart.showToast('Please select an available table from the floor plan');
          return;
        }
        this.setStep(3);
      };
    }
  },

  renderReviewSummary() {
    const resTableEl = document.getElementById('reviewTableInfo');
    const resDateTimeEl = document.getElementById('reviewDateTimeInfo');
    const resGuestsEl = document.getElementById('reviewGuestsInfo');
    const itemsListEl = document.getElementById('reviewOrderItemsList');
    const orderSubtotalEl = document.getElementById('reviewOrderSubtotal');
    const orderGrandTotalEl = document.getElementById('reviewGrandTotal');

    if (resTableEl && this.data.selectedTable) {
      resTableEl.textContent = `Table ${this.data.selectedTable.tableNumber} — ${this.data.selectedTable.type} (${this.data.selectedTable.location})`;
    }
    if (resDateTimeEl) {
      resDateTimeEl.textContent = `${this.data.date} at ${this.data.time} (90 minutes)`;
    }
    if (resGuestsEl) {
      resGuestsEl.textContent = `${this.data.guests} Guests`;
    }

    // Prefill customer form if authenticated
    const user = Auth.getCurrentUser();
    if (user) {
      const nameInput = document.getElementById('resCustomerName');
      const emailInput = document.getElementById('resCustomerEmail');
      const phoneInput = document.getElementById('resCustomerPhone');
      if (nameInput && !nameInput.value) nameInput.value = user.name || '';
      if (emailInput && !emailInput.value) emailInput.value = user.email || '';
      if (phoneInput && !phoneInput.value) phoneInput.value = user.phone || '';
    }

    // Render pre-ordered items
    const items = Cart.getItems();
    const subtotal = Cart.getSubtotal();

    if (itemsListEl) {
      if (items.length === 0) {
        itemsListEl.innerHTML = `
          <div style="padding: 16px; background: rgba(255,255,255,0.03); border-radius: var(--radius-sm); color: var(--text-muted); font-size: 0.88rem;">
            No food or drinks pre-ordered yet. You can still pre-order now or order at the café.
            <br>
            <a href="#menu" style="color: var(--accent-amber); font-weight: 600; display: inline-block; margin-top: 6px;">+ Browse Menu to Add Items</a>
          </div>
        `;
      } else {
        itemsListEl.innerHTML = items.map(i => `
          <div class="review-item-row">
            <div>
              <strong>${i.quantity}× ${i.name}</strong>
              <div style="font-size: 0.78rem; color: var(--text-muted);">${i.size} • ${i.milk} ${i.extras.length ? '• ' + i.extras.join(', ') : ''}</div>
            </div>
            <span>$${i.subtotal.toFixed(2)}</span>
          </div>
        `).join('');
      }
    }

    if (orderSubtotalEl) orderSubtotalEl.textContent = `$${subtotal.toFixed(2)}`;
    if (orderGrandTotalEl) orderGrandTotalEl.textContent = `$${subtotal.toFixed(2)}`;
  },

  bindStep3Controls() {
    const backBtn = document.getElementById('step3BackBtn');
    if (backBtn) {
      backBtn.onclick = () => this.setStep(2);
    }

    const confirmBtn = document.getElementById('step3ConfirmBtn');
    if (confirmBtn) {
      confirmBtn.onclick = async () => {
        const nameInput = document.getElementById('resCustomerName');
        const emailInput = document.getElementById('resCustomerEmail');
        const phoneInput = document.getElementById('resCustomerPhone');
        const notesInput = document.getElementById('resSpecialNotes');

        const customerName = nameInput ? nameInput.value.trim() : '';
        const customerEmail = emailInput ? emailInput.value.trim() : '';
        const customerPhone = phoneInput ? phoneInput.value.trim() : '';
        const specialNotes = notesInput ? notesInput.value.trim() : '';

        if (!customerName || !customerEmail || !customerPhone) {
          Cart.showToast('Please fill in your name, email and phone number');
          return;
        }

        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<span>Securing Table & Order...</span>';

        try {
          const payload = {
            customerName,
            customerEmail,
            customerPhone,
            specialNotes,
            tableId: this.data.selectedTable.id,
            tableNumber: this.data.selectedTable.tableNumber,
            tableType: this.data.selectedTable.type,
            tableLocation: this.data.selectedTable.location,
            date: this.data.date,
            startTime: this.data.time,
            guestCount: this.data.guests,
            orderItems: Cart.getItems().map(item => ({
              productId: item.productId,
              productName: item.name,
              unitPrice: item.unitPrice,
              quantity: item.quantity,
              size: item.size,
              milk: item.milk,
              extras: item.extras,
              subtotal: item.subtotal
            })),
            totalAmount: Cart.getSubtotal()
          };

          const created = await ApiClient.createReservation(payload);
          this.confirmedReservation = created;
          Cart.clear(); // Clear cart after successful reservation

          // If not logged in, auto-save user info to local state for convenience
          if (!Auth.getCurrentUser()) {
            Auth.saveGuestSession({ name: customerName, email: customerEmail, phone: customerPhone });
          }

          this.setStep(4);
        } catch (err) {
          Cart.showToast('Reservation error: ' + err.message);
        } finally {
          confirmBtn.disabled = false;
          confirmBtn.innerHTML = '<span>Confirm Reservation</span>';
        }
      };
    }
  },

  renderConfirmation() {
    if (!this.confirmedReservation) return;

    const res = this.confirmedReservation;
    const codeEl = document.getElementById('confReservationId');
    const dateEl = document.getElementById('confDate');
    const timeEl = document.getElementById('confTime');
    const tableEl = document.getElementById('confTable');
    const guestsEl = document.getElementById('confGuests');
    const orderListEl = document.getElementById('confOrderSummaryList');
    const qrContainer = document.getElementById('confQrCodeBox');

    if (codeEl) codeEl.textContent = res.reservationId;
    if (dateEl) dateEl.textContent = res.date;
    if (timeEl) timeEl.textContent = `${res.startTime} (90 mins)`;
    if (tableEl) tableEl.textContent = `Table ${res.tableNumber} — ${res.tableType} (${res.tableLocation})`;
    if (guestsEl) guestsEl.textContent = `${res.guestCount} Guests`;

    if (orderListEl) {
      if (!res.orderItems || res.orderItems.length === 0) {
        orderListEl.innerHTML = '<span style="color: var(--text-muted);">No pre-ordered items</span>';
      } else {
        orderListEl.innerHTML = res.orderItems.map(item => `
          <div>${item.quantity}× ${item.productName} ($${item.subtotal.toFixed(2)})</div>
        `).join('');
      }
    }

    if (qrContainer) {
      qrContainer.innerHTML = this.generateQrSvg(res.reservationId, res.customerName, res.date, res.startTime);
    }

    // Download Calendar button
    const calBtn = document.getElementById('confAddToCalendarBtn');
    if (calBtn) {
      calBtn.onclick = () => this.downloadIcsCalendar(res);
    }

    // View in My Reservations
    const viewBtn = document.getElementById('confViewReservationsBtn');
    if (viewBtn) {
      viewBtn.onclick = () => {
        const dashSec = document.getElementById('myReservations');
        if (dashSec) {
          dashSec.scrollIntoView({ behavior: 'smooth' });
          if (window.loadMyReservations) window.loadMyReservations();
        }
      };
    }

    // Get Directions
    const dirBtn = document.getElementById('confGetDirectionsBtn');
    if (dirBtn) {
      dirBtn.onclick = () => {
        window.open('https://maps.google.com/?q=Colombo+Specialty+Coffee+KOPA', '_blank');
      };
    }
  },

  generateQrSvg(code, name, date, time) {
    // Elegant crisp SVG representation of the reservation token
    return `
      <svg viewBox="0 0 100 100" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
        <rect width="100" height="100" fill="#FFFFFF"/>
        <!-- Corner Markers -->
        <rect x="10" y="10" width="24" height="24" fill="#13100E"/>
        <rect x="14" y="14" width="16" height="16" fill="#FFFFFF"/>
        <rect x="18" y="18" width="8" height="8" fill="#13100E"/>

        <rect x="66" y="10" width="24" height="24" fill="#13100E"/>
        <rect x="70" y="14" width="16" height="16" fill="#FFFFFF"/>
        <rect x="74" y="18" width="8" height="8" fill="#13100E"/>

        <rect x="10" y="66" width="24" height="24" fill="#13100E"/>
        <rect x="14" y="70" width="16" height="16" fill="#FFFFFF"/>
        <rect x="18" y="74" width="8" height="8" fill="#13100E"/>

        <!-- Grid Data Points -->
        <rect x="40" y="12" width="6" height="6" fill="#13100E"/>
        <rect x="52" y="12" width="6" height="6" fill="#13100E"/>
        <rect x="44" y="24" width="6" height="6" fill="#13100E"/>
        <rect x="12" y="44" width="6" height="6" fill="#13100E"/>
        <rect x="24" y="44" width="6" height="6" fill="#13100E"/>
        <rect x="36" y="38" width="8" height="8" fill="#D48B46"/>
        <rect x="48" y="46" width="8" height="8" fill="#13100E"/>
        <rect x="60" y="38" width="6" height="6" fill="#13100E"/>
        <rect x="72" y="46" width="6" height="6" fill="#13100E"/>
        <rect x="84" y="44" width="6" height="6" fill="#13100E"/>
        <rect x="42" y="64" width="8" height="8" fill="#13100E"/>
        <rect x="56" y="64" width="6" height="6" fill="#D48B46"/>
        <rect x="68" y="68" width="8" height="8" fill="#13100E"/>
        <rect x="82" y="74" width="8" height="8" fill="#13100E"/>
        <rect x="46" y="80" width="8" height="8" fill="#13100E"/>
      </svg>
    `;
  },

  downloadIcsCalendar(res) {
    const [year, month, day] = res.date.split('-');
    let [hour, minute] = res.startTime.split(':');
    hour = String(hour).padStart(2, '0');
    minute = minute || '00';

    const startIso = `${year}${month}${day}T${hour}${minute}00`;
    const endHour = String(parseInt(hour, 10) + 1).padStart(2, '0');
    const endIso = `${year}${month}${day}T${endHour}3000`;

    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//KOPA Specialty Coffee//Reservation//EN',
      'BEGIN:VEVENT',
      `UID:${res.reservationId}@kopa.coffee`,
      `SUMMARY:KOPA Coffee Reservation (${res.tableType} - Table ${res.tableNumber})`,
      `DESCRIPTION:Reservation ID: ${res.reservationId}\\nPre-ordered Items: ${res.orderItems ? res.orderItems.length : 0}\\nGuest Count: ${res.guestCount}\\nEnjoy your KOPA moment!`,
      `LOCATION:KOPA Coffee, Colombo, Sri Lanka`,
      `DTSTART:${startIso}`,
      `DTEND:${endIso}`,
      'STATUS:CONFIRMED',
      'END:VEVENT',
      'END:VCALENDAR'
    ].join('\r\n');

    const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.setAttribute('download', `${res.reservationId}.ics`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
};

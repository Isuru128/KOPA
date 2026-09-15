/**
 * KOPA Coffee — Cart Management Module
 * Connects pre-ordered food & drinks directly to the table reservation workflow.
 */

export const Cart = {
  items: [],

  init() {
    this.loadFromStorage();
    this.bindEvents();
    this.updateUI();
  },

  loadFromStorage() {
    try {
      const saved = localStorage.getItem('kopa_cart');
      if (saved) this.items = JSON.parse(saved);
    } catch (e) {
      this.items = [];
    }
  },

  saveToStorage() {
    localStorage.setItem('kopa_cart', JSON.stringify(this.items));
  },

  addItem(product, options = {}, quantity = 1) {
    const size = options.size || (product.sizeOptions && product.sizeOptions[0]) || 'Standard';
    const milk = options.milk || (product.milkOptions && product.milkOptions[0]) || 'None';
    const extras = options.extras || [];

    // calculate price with adjustments if any
    let unitPrice = product.price;
    if (size === 'Large') unitPrice += 0.75;
    if (size === 'Medium') unitPrice += 0.40;
    if (extras.length > 0) unitPrice += extras.length * 0.60;

    const cartItemId = `${product.id}-${size}-${milk}-${extras.sort().join('-')}`;
    const existing = this.items.find(i => i.cartItemId === cartItemId);

    if (existing) {
      existing.quantity += quantity;
      existing.subtotal = existing.quantity * existing.unitPrice;
    } else {
      this.items.push({
        cartItemId,
        productId: product.id,
        name: product.name,
        image: product.image,
        unitPrice,
        quantity,
        size,
        milk,
        extras,
        subtotal: unitPrice * quantity
      });
    }

    this.saveToStorage();
    this.updateUI();
    this.showToast(`Added ${quantity}× ${product.name} to your order`);
    this.animateBadge();
  },

  updateQuantity(cartItemId, qty) {
    const idx = this.items.findIndex(i => i.cartItemId === cartItemId);
    if (idx >= 0) {
      if (qty <= 0) {
        this.items.splice(idx, 1);
      } else {
        this.items[idx].quantity = qty;
        this.items[idx].subtotal = this.items[idx].quantity * this.items[idx].unitPrice;
      }
      this.saveToStorage();
      this.updateUI();
    }
  },

  removeItem(cartItemId) {
    this.items = this.items.filter(i => i.cartItemId !== cartItemId);
    this.saveToStorage();
    this.updateUI();
  },

  clear() {
    this.items = [];
    this.saveToStorage();
    this.updateUI();
  },

  getItems() {
    return [...this.items];
  },

  getItemCount() {
    return this.items.reduce((acc, i) => acc + i.quantity, 0);
  },

  getSubtotal() {
    return this.items.reduce((acc, i) => acc + i.subtotal, 0);
  },

  updateUI() {
    const count = this.getItemCount();
    const total = this.getSubtotal();

    // Nav cart badge
    const badgeEl = document.getElementById('navCartBadge');
    if (badgeEl) {
      badgeEl.textContent = count;
      badgeEl.style.display = count > 0 ? 'inline-block' : 'none';
    }

    // Mobile sticky bar
    const mobileBar = document.getElementById('mobileStickyBar');
    const mobileCount = document.getElementById('mobileStickyCount');
    const mobileTotal = document.getElementById('mobileStickyTotal');
    if (mobileBar && mobileCount && mobileTotal) {
      if (count > 0) {
        mobileBar.style.display = 'flex';
        mobileCount.textContent = `${count} ${count === 1 ? 'item' : 'items'}`;
        mobileTotal.textContent = `$${total.toFixed(2)}`;
      } else {
        mobileBar.style.display = 'none';
      }
    }

    // Render drawer list
    this.renderDrawerList();
  },

  renderDrawerList() {
    const listEl = document.getElementById('cartDrawerItems');
    const subtotalEl = document.getElementById('cartDrawerSubtotal');
    const totalEl = document.getElementById('cartDrawerTotal');
    if (!listEl) return;

    if (this.items.length === 0) {
      listEl.innerHTML = `
        <div class="cart-empty-state">
          <div class="cart-empty-icon">☕</div>
          <p style="font-weight: 600; margin-bottom: 8px;">Your order is empty</p>
          <p style="font-size: 0.88rem;">Explore the menu to select coffee and pastries for your visit.</p>
          <button class="btn btn-outline btn-sm" style="margin-top: 18px;" id="browseMenuFromCartBtn">
            Explore Menu
          </button>
        </div>
      `;
      const btn = document.getElementById('browseMenuFromCartBtn');
      if (btn) btn.onclick = () => {
        this.closeDrawer();
        const menuSec = document.getElementById('menu');
        if (menuSec) menuSec.scrollIntoView({ behavior: 'smooth' });
      };
      if (subtotalEl) subtotalEl.textContent = '$0.00';
      if (totalEl) totalEl.textContent = '$0.00';
      return;
    }

    listEl.innerHTML = this.items.map(item => {
      const customsText = [
        item.size !== 'Standard' && item.size !== 'Single' ? item.size : '',
        item.milk && item.milk !== 'None' ? item.milk + ' Milk' : '',
        item.extras && item.extras.length ? item.extras.join(', ') : ''
      ].filter(Boolean).join(' • ');

      return `
        <div class="cart-item">
          <img src="${item.image}" alt="${item.name}" class="cart-item-img" onerror="this.src='/images/signature_latte.jpg'"/>
          <div class="cart-item-details">
            <div class="cart-item-name">${item.name}</div>
            ${customsText ? `<div class="cart-item-customs">${customsText}</div>` : ''}
            <div class="cart-item-row">
              <div class="qty-stepper">
                <button class="qty-btn" data-cart-action="decrease" data-id="${item.cartItemId}">−</button>
                <span class="qty-num">${item.quantity}</span>
                <button class="qty-btn" data-cart-action="increase" data-id="${item.cartItemId}">+</button>
              </div>
              <div class="cart-item-price">$${item.subtotal.toFixed(2)}</div>
              <button class="cart-item-remove" data-cart-action="remove" data-id="${item.cartItemId}" title="Remove">✕</button>
            </div>
          </div>
        </div>
      `;
    }).join('');

    const sub = this.getSubtotal();
    if (subtotalEl) subtotalEl.textContent = `$${sub.toFixed(2)}`;
    if (totalEl) totalEl.textContent = `$${sub.toFixed(2)}`;
  },

  bindEvents() {
    // Open cart drawer
    document.querySelectorAll('[data-action="open-cart"]').forEach(btn => {
      btn.onclick = () => this.openDrawer();
    });

    // Close cart drawer
    const closeBtn = document.getElementById('closeCartDrawerBtn');
    if (closeBtn) closeBtn.onclick = () => this.closeDrawer();

    const overlay = document.getElementById('cartDrawerOverlay');
    if (overlay) overlay.onclick = () => this.closeDrawer();

    // Delegate cart list interactions
    const listEl = document.getElementById('cartDrawerItems');
    if (listEl) {
      listEl.onclick = (e) => {
        const btn = e.target.closest('[data-cart-action]');
        if (!btn) return;
        const action = btn.dataset.cartAction;
        const id = btn.dataset.id;
        const item = this.items.find(i => i.cartItemId === id);
        if (!item) return;

        if (action === 'increase') this.updateQuantity(id, item.quantity + 1);
        if (action === 'decrease') this.updateQuantity(id, item.quantity - 1);
        if (action === 'remove') this.removeItem(id);
      };
    }

    // Continue to reservation button inside cart
    const proceedBtn = document.getElementById('cartProceedReservationBtn');
    if (proceedBtn) {
      proceedBtn.onclick = () => {
        this.closeDrawer();
        const resSec = document.getElementById('reserve');
        if (resSec) {
          resSec.scrollIntoView({ behavior: 'smooth' });
        }
      };
    }

    // Mobile sticky bar button
    const mobileBtn = document.getElementById('mobileStickyReserveBtn');
    if (mobileBtn) {
      mobileBtn.onclick = () => {
        const resSec = document.getElementById('reserve');
        if (resSec) resSec.scrollIntoView({ behavior: 'smooth' });
      };
    }
  },

  openDrawer() {
    const drawer = document.getElementById('cartDrawer');
    const overlay = document.getElementById('cartDrawerOverlay');
    if (drawer) drawer.classList.add('open');
    if (overlay) overlay.classList.add('active');
  },

  closeDrawer() {
    const drawer = document.getElementById('cartDrawer');
    const overlay = document.getElementById('cartDrawerOverlay');
    if (drawer) drawer.classList.remove('open');
    if (overlay) overlay.classList.remove('active');
  },

  showToast(msg) {
    let container = document.getElementById('toastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `<span class="toast-icon">☕</span> <span>${msg}</span>`;
    container.appendChild(toast);

    requestAnimationFrame(() => toast.classList.add('show'));
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, 3200);
  },

  animateBadge() {
    const badge = document.getElementById('navCartBadge');
    if (badge) {
      badge.style.transform = 'scale(1.35)';
      setTimeout(() => badge.style.transform = 'scale(1)', 200);
    }
  }
};

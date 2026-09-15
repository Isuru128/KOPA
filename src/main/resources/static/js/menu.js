/**
 * KOPA Coffee — Menu & Product Customization Module
 */

import { ApiClient } from './api.js';
import { Cart } from './cart.js';

export const Menu = {
  allProducts: [],
  filteredProducts: [],
  selectedCategory: 'All',
  activeProduct: null,
  activeOptions: {
    size: '',
    milk: '',
    extras: []
  },
  modalQuantity: 1,

  async init() {
    await this.loadProducts();
    this.renderCategoryTabs();
    this.bindSearch();
    this.bindModalEvents();
  },

  async loadProducts() {
    this.allProducts = await ApiClient.getProducts();
    this.filterAndRender();
  },

  renderCategoryTabs() {
    const tabsContainer = document.getElementById('categoryTabs');
    if (!tabsContainer) return;

    const categories = ['All', 'Coffee', 'Cold Drinks', 'Tea', 'Breakfast', 'Pastries', 'Desserts', 'Snacks'];
    tabsContainer.innerHTML = categories.map(cat => `
      <button class="tab-btn ${cat === this.selectedCategory ? 'active' : ''}" data-category="${cat}">
        ${cat}
      </button>
    `).join('');

    tabsContainer.onclick = (e) => {
      const btn = e.target.closest('.tab-btn');
      if (!btn) return;
      tabsContainer.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      this.selectedCategory = btn.dataset.category;
      this.filterAndRender();
    };
  },

  bindSearch() {
    const searchInput = document.getElementById('menuSearchInput');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
      const q = e.target.value.toLowerCase().trim();
      this.filterAndRender(q);
    });
  },

  filterAndRender(searchQuery = '') {
    let list = this.allProducts;
    if (this.selectedCategory !== 'All') {
      list = list.filter(p => p.category.toLowerCase() === this.selectedCategory.toLowerCase());
    }
    if (searchQuery) {
      list = list.filter(p => 
        p.name.toLowerCase().includes(searchQuery) ||
        p.description.toLowerCase().includes(searchQuery)
      );
    }
    this.filteredProducts = list;
    this.renderProducts();
  },

  renderProducts() {
    const grid = document.getElementById('productGrid');
    if (!grid) return;

    if (this.filteredProducts.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-muted);">
          <p style="font-size: 1.1rem; margin-bottom: 8px;">No items found matching your criteria.</p>
          <button class="btn btn-secondary btn-sm" id="resetMenuFilterBtn">Clear Filters</button>
        </div>
      `;
      const resetBtn = document.getElementById('resetMenuFilterBtn');
      if (resetBtn) {
        resetBtn.onclick = () => {
          this.selectedCategory = 'All';
          this.renderCategoryTabs();
          const search = document.getElementById('menuSearchInput');
          if (search) search.value = '';
          this.filterAndRender();
        };
      }
      return;
    }

    grid.innerHTML = this.filteredProducts.map(p => {
      return `
        <div class="product-card" data-product-id="${p.id}">
          <div class="product-image-container" data-action="open-detail">
            <img src="${p.image}" alt="${p.name}" loading="lazy" onerror="this.src='/images/signature_latte.jpg'"/>
            ${p.dietary ? `<span class="dietary-tag">${p.dietary}</span>` : ''}
          </div>
          <div class="product-info">
            <div class="product-meta">
              <h3 class="product-title" data-action="open-detail">${p.name}</h3>
              <span class="product-price">$${p.price.toFixed(2)}</span>
            </div>
            <p class="product-desc">${p.description}</p>
            <div class="product-actions">
              <div class="qty-stepper">
                <button class="qty-btn" data-qty-action="dec">−</button>
                <span class="qty-num" id="qty-${p.id}">1</span>
                <button class="qty-btn" data-qty-action="inc">+</button>
              </div>
              <button class="btn-add-order" data-action="quick-add">
                <span>+ Add to Order</span>
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');

    // Delegate grid clicks
    grid.onclick = (e) => {
      const card = e.target.closest('.product-card');
      if (!card) return;
      const pid = card.dataset.productId;
      const prod = this.allProducts.find(p => p.id === pid);
      if (!prod) return;

      const qtySpan = card.querySelector(`#qty-${pid}`);
      let qty = parseInt(qtySpan ? qtySpan.textContent : '1', 10);

      // Quantity buttons
      const qtyBtn = e.target.closest('[data-qty-action]');
      if (qtyBtn) {
        if (qtyBtn.dataset.qtyAction === 'inc') qty++;
        if (qtyBtn.dataset.qtyAction === 'dec' && qty > 1) qty--;
        if (qtySpan) qtySpan.textContent = qty;
        return;
      }

      // Quick add button
      if (e.target.closest('[data-action="quick-add"]')) {
        // If product has multiple sizes or milk options, open modal for customization!
        if ((prod.sizeOptions && prod.sizeOptions.length > 1) || (prod.milkOptions && prod.milkOptions.length > 1)) {
          this.openProductModal(prod, qty);
        } else {
          Cart.addItem(prod, {}, qty);
        }
        return;
      }

      // Open detail modal on image or title click
      if (e.target.closest('[data-action="open-detail"]')) {
        this.openProductModal(prod, qty);
      }
    };
  },

  openProductModal(product, initialQty = 1) {
    this.activeProduct = product;
    this.modalQuantity = initialQty;
    this.activeOptions = {
      size: (product.sizeOptions && product.sizeOptions[0]) || '',
      milk: (product.milkOptions && product.milkOptions[0]) || '',
      extras: []
    };

    const modal = document.getElementById('productDetailModal');
    const imgEl = document.getElementById('modalProductImage');
    const titleEl = document.getElementById('modalProductTitle');
    const descEl = document.getElementById('modalProductDesc');
    const priceEl = document.getElementById('modalProductPrice');
    const bodyEl = document.getElementById('modalCustomizationBody');
    const qtyEl = document.getElementById('modalQtyNum');

    if (!modal) return;

    if (imgEl) {
      imgEl.src = product.image;
      imgEl.onerror = () => { imgEl.src = '/images/signature_latte.jpg'; };
    }
    if (titleEl) titleEl.textContent = product.name;
    if (descEl) descEl.textContent = product.description;
    if (qtyEl) qtyEl.textContent = this.modalQuantity;

    // Render Options
    let customHtml = '';

    // Sizes
    if (product.sizeOptions && product.sizeOptions.length > 0) {
      customHtml += `
        <div class="customization-section">
          <div class="customization-title">Select Size</div>
          <div class="pill-group" id="modalSizePills">
            ${product.sizeOptions.map((s, idx) => `
              <button class="custom-pill ${idx === 0 ? 'selected' : ''}" data-size="${s}">
                ${s} ${s === 'Large' ? '(+$0.75)' : s === 'Medium' ? '(+$0.40)' : ''}
              </button>
            `).join('')}
          </div>
        </div>
      `;
    }

    // Milk
    if (product.milkOptions && product.milkOptions.length > 0) {
      customHtml += `
        <div class="customization-section">
          <div class="customization-title">Choice of Milk</div>
          <div class="pill-group" id="modalMilkPills">
            ${product.milkOptions.map((m, idx) => `
              <button class="custom-pill ${idx === 0 ? 'selected' : ''}" data-milk="${m}">
                ${m}
              </button>
            `).join('')}
          </div>
        </div>
      `;
    }

    // Extras
    if (product.extraOptions && product.extraOptions.length > 0) {
      customHtml += `
        <div class="customization-section">
          <div class="customization-title">Add Extras (+$0.60 each)</div>
          <div class="pill-group" id="modalExtrasPills">
            ${product.extraOptions.map(ex => `
              <button class="custom-pill" data-extra="${ex}">
                + ${ex}
              </button>
            `).join('')}
          </div>
        </div>
      `;
    }

    if (bodyEl) bodyEl.innerHTML = customHtml;

    this.updateModalPrice();
    modal.classList.add('active');
  },

  updateModalPrice() {
    const priceEl = document.getElementById('modalProductPrice');
    if (!priceEl || !this.activeProduct) return;

    let total = this.activeProduct.price;
    if (this.activeOptions.size === 'Large') total += 0.75;
    if (this.activeOptions.size === 'Medium') total += 0.40;
    total += this.activeOptions.extras.length * 0.60;

    const grandTotal = total * this.modalQuantity;
    priceEl.textContent = `$${grandTotal.toFixed(2)}`;
  },

  bindModalEvents() {
    const modal = document.getElementById('productDetailModal');
    if (!modal) return;

    // Close buttons
    const closeBtn = document.getElementById('closeModalBtn');
    if (closeBtn) closeBtn.onclick = () => modal.classList.remove('active');
    modal.onclick = (e) => {
      if (e.target === modal) modal.classList.remove('active');
    };

    // Body interactions (pills)
    const bodyEl = document.getElementById('modalCustomizationBody');
    if (bodyEl) {
      bodyEl.onclick = (e) => {
        const pill = e.target.closest('.custom-pill');
        if (!pill) return;

        // Size
        if (pill.dataset.size) {
          const group = document.getElementById('modalSizePills');
          group.querySelectorAll('.custom-pill').forEach(p => p.classList.remove('selected'));
          pill.classList.add('selected');
          this.activeOptions.size = pill.dataset.size;
          this.updateModalPrice();
        }
        // Milk
        else if (pill.dataset.milk) {
          const group = document.getElementById('modalMilkPills');
          group.querySelectorAll('.custom-pill').forEach(p => p.classList.remove('selected'));
          pill.classList.add('selected');
          this.activeOptions.milk = pill.dataset.milk;
          this.updateModalPrice();
        }
        // Extras (multi-select toggle)
        else if (pill.dataset.extra) {
          const extraVal = pill.dataset.extra;
          if (pill.classList.contains('selected')) {
            pill.classList.remove('selected');
            this.activeOptions.extras = this.activeOptions.extras.filter(x => x !== extraVal);
          } else {
            pill.classList.add('selected');
            this.activeOptions.extras.push(extraVal);
          }
          this.updateModalPrice();
        }
      };
    }

    // Quantity stepper inside modal
    const decBtn = document.getElementById('modalQtyDec');
    const incBtn = document.getElementById('modalQtyInc');
    const qtyNum = document.getElementById('modalQtyNum');

    if (decBtn) {
      decBtn.onclick = () => {
        if (this.modalQuantity > 1) {
          this.modalQuantity--;
          if (qtyNum) qtyNum.textContent = this.modalQuantity;
          this.updateModalPrice();
        }
      };
    }
    if (incBtn) {
      incBtn.onclick = () => {
        this.modalQuantity++;
        if (qtyNum) qtyNum.textContent = this.modalQuantity;
        this.updateModalPrice();
      };
    }

    // Add to order CTA
    const addBtn = document.getElementById('modalAddToCartBtn');
    if (addBtn) {
      addBtn.onclick = () => {
        if (!this.activeProduct) return;
        Cart.addItem(this.activeProduct, this.activeOptions, this.modalQuantity);
        modal.classList.remove('active');
      };
    }
  }
};

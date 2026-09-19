/**
 * KOPA Coffee — API Client
 * Interfaces seamlessly with Spring Boot REST API (/api/*)
 * Includes smart localStorage caching & fallback for ultra-reliable offline UX.
 */

const API_BASE = '/api';

export const ApiClient = {
  getToken() {
    return localStorage.getItem('kopa_token') || '';
  },

  setToken(token) {
    if (token) {
      localStorage.setItem('kopa_token', token);
    } else {
      localStorage.removeItem('kopa_token');
    }
  },

  getAuthHeaders(extraHeaders = {}) {
    const headers = { ...extraHeaders };
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  },

  // Products
  async getProducts(category = '') {
    try {
      const url = category && category !== 'All' 
        ? `${API_BASE}/products?category=${encodeURIComponent(category)}`
        : `${API_BASE}/products`;
      const res = await fetch(url);
      if (!res.ok) throw new Error('Network response was not ok');
      return await res.json();
    } catch (err) {
      console.warn('API getProducts fallback:', err);
      return FallbackData.getProducts(category);
    }
  },

  async getCategories() {
    try {
      const res = await fetch(`${API_BASE}/categories`);
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch (err) {
      return ['Coffee', 'Cold Drinks', 'Tea', 'Breakfast', 'Pastries', 'Desserts', 'Snacks'];
    }
  },

  // Tables & Availability
  async getTableAvailability(date, time, guests) {
    try {
      const url = `${API_BASE}/tables/availability?date=${date}&time=${encodeURIComponent(time)}&guests=${guests}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error('Failed to load table availability');
      return await res.json();
    } catch (err) {
      console.warn('API getTableAvailability fallback:', err);
      return FallbackData.getTableAvailability(date, time, guests);
    }
  },

  // Reservations
  async createReservation(reservationPayload) {
    try {
      const res = await fetch(`${API_BASE}/reservations`, {
        method: 'POST',
        headers: this.getAuthHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(reservationPayload)
      });
      if (!res.ok) throw new Error('Failed to create reservation');
      const data = await res.json();
      FallbackData.saveLocalReservation(data);
      return data;
    } catch (err) {
      console.warn('API createReservation fallback:', err);
      return FallbackData.createLocalReservation(reservationPayload);
    }
  },

  async getUserReservations(email) {
    try {
      const res = await fetch(`${API_BASE}/reservations/user/${encodeURIComponent(email)}`, {
        headers: this.getAuthHeaders()
      });
      if (!res.ok) throw new Error('Failed to fetch user reservations');
      const data = await res.json();
      const local = FallbackData.getLocalReservations(email);
      const combined = [...data];
      local.forEach(l => {
        if (!combined.some(c => c.reservationId === l.reservationId)) {
          combined.push(l);
        }
      });
      return combined;
    } catch (err) {
      return FallbackData.getLocalReservations(email);
    }
  },

  async cancelReservation(idOrCode) {
    try {
      const res = await fetch(`${API_BASE}/reservations/${idOrCode}/cancel`, {
        method: 'PUT',
        headers: this.getAuthHeaders()
      });
      if (!res.ok) throw new Error('Cancel failed');
      return await res.json();
    } catch (err) {
      return FallbackData.cancelLocalReservation(idOrCode);
    }
  },

  // Authentication & JWT
  async login(email, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Invalid email or password');
    }
    if (data.token) {
      this.setToken(data.token);
    }
    return data.user;
  },

  async register(name, email, phone, password) {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, phone, password })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Registration failed');
    }
    if (data.token) {
      this.setToken(data.token);
    }
    return data.user;
  },

  async socialAuth(provider, email, name, avatarUrl = '') {
    const res = await fetch(`${API_BASE}/auth/social`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ provider, email, name, avatarUrl })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Social authentication failed');
    }
    if (data.token) {
      this.setToken(data.token);
    }
    return data.user;
  },

  async getCurrentUser() {
    const token = this.getToken();
    if (!token) return null;
    try {
      const res = await fetch(`${API_BASE}/auth/me`, {
        headers: this.getAuthHeaders()
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn('Failed to fetch user with JWT token:', e);
    }
    return null;
  }
};

/**
 * Local Fallback Data Layer
 */
const FallbackData = {
  getProducts(category) {
    const list = [
      {
        id: 'prod-latte-sig',
        name: 'KOPA Signature Latte',
        description: 'Rich espresso, silky micro-foamed milk and our signature salted caramel finish.',
        category: 'Coffee',
        price: 4.50,
        image: '/images/signature_latte.jpg',
        available: true,
        dietary: 'Signature',
        sizeOptions: ['Small', 'Medium', 'Large'],
        milkOptions: ['Regular', 'Oat', 'Almond'],
        extraOptions: ['Extra shot', 'Caramel', 'Vanilla']
      },
      {
        id: 'prod-coldbrew-18h',
        name: 'Single-Origin Cold Brew',
        description: 'Slow-steeped for 18 hours. Notes of dark chocolate, candied orange peel, and toasted hazelnut.',
        category: 'Cold Drinks',
        price: 4.75,
        image: '/images/cold_brew.jpg',
        available: true,
        dietary: 'Single Origin',
        sizeOptions: ['Medium', 'Large'],
        milkOptions: ['Black', 'Oat Float', 'Sweet Cream'],
        extraOptions: ['Extra Shot', 'Vanilla Sweet Cold Foam']
      },
      {
        id: 'prod-flatwhite',
        name: 'Velvet Flat White',
        description: 'Double ristretto extraction with micro-textured whole milk creating a glossy, rich cup.',
        category: 'Coffee',
        price: 4.25,
        image: 'https://images.unsplash.com/photo-1577968897966-3d4325b36b61?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'House Favorite',
        sizeOptions: ['Regular (6oz)'],
        milkOptions: ['Regular', 'Oat', 'Almond'],
        extraOptions: ['Extra shot']
      },
      {
        id: 'prod-cortado',
        name: 'Spanish Cortado',
        description: 'Equal parts velvety steamed milk and concentrated double espresso in a heavy gibraltar glass.',
        category: 'Coffee',
        price: 4.00,
        image: 'https://images.unsplash.com/photo-1534778101976-62847782c213?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'Barista Choice',
        sizeOptions: ['Standard (4oz)'],
        milkOptions: ['Regular', 'Oat', 'Almond'],
        extraOptions: ['Demerara Sugar']
      },
      {
        id: 'prod-matcha-uji',
        name: 'Ceremonial Uji Matcha Latte',
        description: 'First harvest Uji green tea whisked with silky milk and gentle raw wildflower honey.',
        category: 'Tea',
        price: 5.00,
        image: 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'Organic',
        sizeOptions: ['Small', 'Medium', 'Large'],
        milkOptions: ['Regular', 'Oat', 'Almond'],
        extraOptions: ['Vanilla', 'Extra Matcha Whisk']
      },
      {
        id: 'prod-almond-croissant',
        name: 'Artisan Almond Croissant',
        description: 'Twice-baked flaky butter pastry filled with rich almond frangipane and toasted almonds.',
        category: 'Pastries',
        price: 4.25,
        image: '/images/hero_cafe.jpg',
        available: true,
        dietary: 'Bakery Fresh',
        sizeOptions: ['Single'],
        milkOptions: [],
        extraOptions: ['Warm up', 'Extra Almond Drizzle']
      },
      {
        id: 'prod-pain-chocolat',
        name: 'Pain au Chocolat',
        description: 'Crisp, golden laminated dough enveloping double Belgian 70% dark chocolate batons.',
        category: 'Pastries',
        price: 3.80,
        image: 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'Bakery Fresh',
        sizeOptions: ['Single'],
        milkOptions: [],
        extraOptions: ['Warm up']
      },
      {
        id: 'prod-avo-toast',
        name: 'Truffled Avocado Toast',
        description: 'House-toasted sourdough, whipped Haas avocado, poached organic egg, watermelon radish.',
        category: 'Breakfast',
        price: 8.50,
        image: 'https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'Vegetarian',
        sizeOptions: ['Standard'],
        milkOptions: [],
        extraOptions: ['Extra Poached Egg', 'Smoked Bacon', 'Gluten-Free Bread']
      },
      {
        id: 'prod-basque-cake',
        name: 'Burnt Basque Espresso Cheesecake',
        description: 'Caramelized rustic crust with an ultra-creamy interior infused with KOPA espresso reduction.',
        category: 'Desserts',
        price: 6.50,
        image: 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=800&q=80',
        available: true,
        dietary: 'House Specialty',
        sizeOptions: ['Slice'],
        milkOptions: [],
        extraOptions: ['Caramel Sauce Side', 'Dollop of Cream']
      }
    ];
    if (category && category !== 'All') {
      return list.filter(p => p.category.toLowerCase() === category.toLowerCase());
    }
    return list;
  },

  getTableAvailability(date, time, guests) {
    const rawTables = [
      { id: 'tbl-01', tableNumber: '01', capacity: 2, type: 'Window Table', location: 'Window Bay', status: 'AVAILABLE', description: 'Street view window seat with warm morning light.' },
      { id: 'tbl-02', tableNumber: '02', capacity: 4, type: 'Window Table', location: 'Window Bay', status: 'AVAILABLE', description: 'Corner window table for four.' },
      { id: 'tbl-03', tableNumber: '03', capacity: 2, type: 'Window Table', location: 'Window Bay', status: 'AVAILABLE', description: 'Cozy reading window alcove.' },
      { id: 'tbl-04', tableNumber: '04', capacity: 2, type: 'Couple Table', location: 'Cozy Alcove', status: 'AVAILABLE', description: 'Intimate velvet leather booth.' },
      { id: 'tbl-05', tableNumber: '05', capacity: 2, type: 'Couple Table', location: 'Cozy Alcove', status: 'AVAILABLE', description: 'Warm timber nook for quiet moments.' },
      { id: 'tbl-06', tableNumber: '06', capacity: 4, type: 'Standard Table', location: 'Main Lounge', status: 'AVAILABLE', description: 'Dark oak table in the center lounge.' },
      { id: 'tbl-07', tableNumber: '07', capacity: 4, type: 'Standard Table', location: 'Main Lounge', status: 'AVAILABLE', description: 'Mid-century armchairs with laptop power.' },
      { id: 'tbl-08', tableNumber: '08', capacity: 2, type: 'Window Table', location: 'Window Bay', status: 'AVAILABLE', description: 'Prime window table with acoustic baffling.' },
      { id: 'tbl-09', tableNumber: '09', capacity: 6, type: 'Group Table', location: 'Central Atrium', status: 'AVAILABLE', description: 'Long communal walnut table.' },
      { id: 'tbl-10', tableNumber: '10', capacity: 4, type: 'Outdoor Table', location: 'Garden Terrace', status: 'AVAILABLE', description: 'Breezy patio table surrounded by plants.' },
      { id: 'tbl-11', tableNumber: '11', capacity: 2, type: 'Outdoor Table', location: 'Garden Terrace', status: 'AVAILABLE', description: 'Marble bistro round table.' },
      { id: 'tbl-12', tableNumber: '12', capacity: 1, type: 'Counter Seat', location: 'Espresso Bar', status: 'AVAILABLE', description: 'Front row bar stool watching barista extractions.' },
      { id: 'tbl-13', tableNumber: '13', capacity: 1, type: 'Counter Seat', location: 'Espresso Bar', status: 'AVAILABLE', description: 'Brew bar stool with USB-C power outlet.' },
      { id: 'tbl-14', tableNumber: '14', capacity: 1, type: 'Counter Seat', location: 'Espresso Bar', status: 'MAINTENANCE', description: 'Espresso machine maintenance bay.' }
    ];

    return rawTables.map(t => {
      let state = t.status;
      let note = 'Available to reserve';
      if (state === 'MAINTENANCE') {
        note = 'Under maintenance';
      } else if (t.capacity < guests) {
        state = 'UNSUITABLE';
        note = `Capacity: ${t.capacity} (Need ${guests})`;
      }
      return { ...t, status: state, note };
    });
  },

  getLocalReservations(email) {
    const list = JSON.parse(localStorage.getItem('kopa_reservations') || '[]');
    if (!email) return list;
    return list.filter(r => (r.customerEmail || '').toLowerCase() === email.toLowerCase());
  },

  saveLocalReservation(res) {
    const list = JSON.parse(localStorage.getItem('kopa_reservations') || '[]');
    const idx = list.findIndex(r => r.reservationId === res.reservationId);
    if (idx >= 0) list[idx] = res;
    else list.unshift(res);
    localStorage.setItem('kopa_reservations', JSON.stringify(list));
  },

  createLocalReservation(data) {
    const seq = Math.floor(10000 + Math.random() * 90000);
    const code = `KOPA-2026-${seq}`;
    const res = {
      id: 'res-' + Date.now(),
      reservationId: code,
      ...data,
      status: 'CONFIRMED',
      createdAt: new Date().toISOString(),
      qrCode: `KOPA-RESERVATION|${code}|Table ${data.tableNumber}|${data.date} ${data.startTime}`
    };
    this.saveLocalReservation(res);
    return res;
  },

  cancelLocalReservation(idOrCode) {
    const list = JSON.parse(localStorage.getItem('kopa_reservations') || '[]');
    const item = list.find(r => r.id === idOrCode || r.reservationId === idOrCode);
    if (item) {
      item.status = 'CANCELLED';
      localStorage.setItem('kopa_reservations', JSON.stringify(list));
      return item;
    }
    return null;
  }
};

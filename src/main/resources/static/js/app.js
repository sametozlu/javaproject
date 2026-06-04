const API = '';
const STORAGE_TOKEN = 'shopflow_token';
const STORAGE_REFRESH = 'shopflow_refresh';
const STORAGE_USER = 'shopflow_user';
const STORAGE_CART = 'shopflow_cart';
const DEMO_CARD_SUCCESS = '4242424242424242';
const DEMO_CARD_DECLINE = '4000000000000002';

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

function loadCart() {
    try { return JSON.parse(localStorage.getItem(STORAGE_CART) || '[]'); } catch { return []; }
}
function loadUser() {
    try { return JSON.parse(localStorage.getItem(STORAGE_USER) || 'null'); } catch { return null; }
}

const state = {
    products: [],
    categories: [],
    wishlist: [],
    cart: loadCart(),
    user: loadUser(),
    token: localStorage.getItem(STORAGE_TOKEN),
    refreshToken: localStorage.getItem(STORAGE_REFRESH),
    search: '',
    minPrice: null,
    maxPrice: null,
    categoryId: null,
    page: 0,
    totalPages: 0,
    sort: 'newest',
    heroSlide: 0,
    heroTimer: null,
    addresses: [],
    selectedAddressId: null,
    selectedProductId: null,
    appliedCoupon: null,
    pendingPaymentOrder: null,
    chart: null,
};

const SORT_MAP = {
    newest: 'createdAt,desc',
    'price-asc': 'price,asc',
    'price-desc': 'price,desc',
    name: 'name,asc',
};

const ORDER_STEPS = [
    { key: 'PENDING', label: 'Sipariş alındı' },
    { key: 'CONFIRMED', label: 'Onaylandı' },
    { key: 'SHIPPED', label: 'Kargoda' },
    { key: 'DELIVERED', label: 'Teslim edildi' },
];

function escapeHtml(t) {
    if (t == null) return '';
    return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function formatMoney(n) {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(Number(n) || 0);
}
function toast(msg, type = 'success') {
    const el = $('#toast');
    if (!el) return;
    el.textContent = msg;
    el.className = `toast show ${type}`;
    clearTimeout(toast._t);
    toast._t = setTimeout(() => { el.className = 'toast'; }, 3500);
}
function saveCart() {
    localStorage.setItem(STORAGE_CART, JSON.stringify(state.cart));
    updateCartUI();
}
function getProductImage(p) {
    if (p.imageUrl) return p.imageUrl;
    return `https://picsum.photos/seed/shopflow-${encodeURIComponent(`${p.id}-${p.name || 'p'}`)}/600/450`;
}
function getProductGallery(p) {
    const main = getProductImage(p);
    if (p.imageUrl) return [main, `${p.imageUrl}?v=2`, `${p.imageUrl}?v=3`].filter((v, i, a) => a.indexOf(v) === i);
    return [
        main,
        `https://picsum.photos/seed/shopflow-${p.id}-b/600/450`,
        `https://picsum.photos/seed/shopflow-${p.id}-c/600/450`,
    ];
}

const ADMIN_STAT_LABELS = {
    totalUsers: 'Kullanıcılar',
    totalProducts: 'Ürünler',
    totalOrders: 'Siparişler',
    pendingOrders: 'Bekleyen',
    totalRevenue: 'Toplam gelir',
};
const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
function renderStars(rating) {
    const r = Math.round(Number(rating) || 0);
    return '★'.repeat(r) + '☆'.repeat(5 - r);
}
function renderOrderTimeline(status) {
    const cancelled = status === 'CANCELLED';
    const activeIdx = cancelled ? -1 : ORDER_STEPS.findIndex((s) => s.key === status);
    const steps = cancelled ? [{ key: 'CANCELLED', label: 'İptal edildi' }] : ORDER_STEPS;
    return `<div class="order-timeline ${cancelled ? 'cancelled' : ''}">${steps.map((step, i) => {
        const done = !cancelled && i <= activeIdx;
        const active = !cancelled && i === activeIdx;
        return `<div class="timeline-step ${done ? 'done' : ''} ${active ? 'active' : ''}"><span class="dot"></span><span>${step.label}</span></div>`;
    }).join('')}</div>`;
}

async function tryRefreshToken() {
    if (!state.refreshToken) return false;
    try {
        const res = await fetch(`${API}/api/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: state.refreshToken }),
        });
        if (!res.ok) return false;
        const data = await res.json();
        state.token = data.token;
        state.refreshToken = data.refreshToken || state.refreshToken;
        localStorage.setItem(STORAGE_TOKEN, state.token);
        if (data.refreshToken) localStorage.setItem(STORAGE_REFRESH, data.refreshToken);
        return true;
    } catch { return false; }
}

async function api(path, options = {}, retried = false) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    const res = await fetch(`${API}${path}`, { ...options, headers });
    const text = await res.text();
    let data = null;
    if (text) {
        try { data = JSON.parse(text); } catch { data = { message: text }; }
    }
    if (!res.ok) {
        if (res.status === 401 && !retried && await tryRefreshToken()) return api(path, options, true);
        const msg = data?.message || data?.error || `Hata ${res.status}`;
        const details = data?.details?.join?.(', ');
        throw new Error(details ? `${msg}: ${details}` : msg);
    }
    return data;
}

function isPaymentPaid(pay) {
    return String(pay?.status || '').toUpperCase() === 'PAID';
}
function digitsOnly(str) { return (str || '').replace(/\D/g, ''); }
function formatCardNumber(value) {
    const d = digitsOnly(value).slice(0, 16);
    return d.replace(/(\d{4})(?=\d)/g, '$1 ').trim();
}
function formatCardExpiry(value) {
    const d = digitsOnly(value).slice(0, 4);
    if (d.length <= 2) return d;
    return `${d.slice(0, 2)}/${d.slice(2)}`;
}
function shouldSimulatePaymentFail(cardNumber) {
    if ($('#simulatePaymentFail')?.checked) return true;
    return digitsOnly(cardNumber) === DEMO_CARD_DECLINE;
}
function detectCardBrand(digits) {
    if (!digits || digits.length < 1) return 'default-card';
    if (digits.startsWith('4')) return 'visa';
    if (/^5[1-5]/.test(digits) || /^2[2-7]/.test(digits)) return 'mastercard';
    if (/^3[47]/.test(digits)) return 'amex';
    return 'default-card';
}
function getCardBrandLabel(brand) {
    const map = { visa: 'VISA', mastercard: 'MC', amex: 'AMEX', 'default-card': 'CARD' };
    return map[brand] || 'CARD';
}
function fillDemoCard(success) {
    if (success) {
        if ($('#cardNumber')) $('#cardNumber').value = formatCardNumber(DEMO_CARD_SUCCESS);
        if ($('#simulatePaymentFail')) $('#simulatePaymentFail').checked = false;
    } else {
        if ($('#cardNumber')) $('#cardNumber').value = formatCardNumber('4000 0000 0000 0002');
        if ($('#simulatePaymentFail')) $('#simulatePaymentFail').checked = true;
    }
    if ($('#cardName')) $('#cardName').value = (state.user?.fullName || 'DEMO USER').toUpperCase();
    if ($('#cardExpiry')) $('#cardExpiry').value = '12/28';
    if ($('#cardCvc')) $('#cardCvc').value = '123';
    updateCardPreview();
}
function updateCardPreview() {
    const num = $('#cardNumber')?.value || '•••• •••• •••• ••••';
    const name = ($('#cardName')?.value || 'AD SOYAD').toUpperCase();
    const exp = $('#cardExpiry')?.value || 'MM/YY';
    const digits = digitsOnly($('#cardNumber')?.value);
    const brand = detectCardBrand(digits);
    const preview = $('#cardPreview');
    if (preview) {
        preview.className = `card-preview ${brand}`;
    }
    if ($('#cardPreviewBrand')) $('#cardPreviewBrand').textContent = getCardBrandLabel(brand);
    if ($('#cardPreviewNumber')) $('#cardPreviewNumber').textContent = num || '•••• •••• •••• ••••';
    if ($('#cardPreviewName')) $('#cardPreviewName').textContent = name;
    if ($('#cardPreviewExpiry')) $('#cardPreviewExpiry').textContent = exp;
}
function openPaymentModal(order) {
    state.pendingPaymentOrder = order;
    const amount = formatMoney(order.totalAmount);
    $('#paymentOrderId').textContent = `#${order.id}`;
    $('#paymentOrderAmount').textContent = amount;
    if ($('#paymentSubmitAmount')) $('#paymentSubmitAmount').textContent = amount;
    const form = $('#paymentForm');
    if (form) {
        form.reset();
        if ($('#simulatePaymentFail')) $('#simulatePaymentFail').checked = false;
    }
    fillDemoCard(true);
    closeCart();
    $('#paymentModal').showModal();
}
async function processPayment(orderId) {
    const cardNumber = digitsOnly($('#cardNumber')?.value);
    if (cardNumber.length < 16) { toast('Geçerli bir kart numarası girin (16 hane)', 'error'); return; }
    if (digitsOnly($('#cardExpiry')?.value).length < 4) { toast('Son kullanma tarihi girin (AA/YY)', 'error'); return; }
    if (digitsOnly($('#cardCvc')?.value).length < 3) { toast('CVV girin', 'error'); return; }
    if (!$('#cardName')?.value?.trim()) { toast('Kart üzerindeki ismi girin', 'error'); return; }

    const btn = $('#confirmPaymentBtn');
    const simulateFailure = shouldSimulatePaymentFail(cardNumber);
    if (btn) { btn.disabled = true; btn.textContent = 'İşleniyor...'; }
    try {
        await new Promise((r) => setTimeout(r, 900));
        const pay = await api(`/api/orders/${orderId}/pay`, {
            method: 'POST',
            body: JSON.stringify({
                idempotencyKey: `pay-${orderId}-${Date.now()}`,
                simulateFailure: simulateFailure === true,
            }),
        });
        $('#paymentModal')?.close();
        state.pendingPaymentOrder = null;
        if (isPaymentPaid(pay)) {
            state.cart = [];
            state.appliedCoupon = null;
            saveCart();
            toast('Ödeme başarılı! Siparişin onaylandı.');
        } else {
            toast(pay?.message || 'Ödeme reddedildi. Siparişlerimden tekrar deneyebilirsin.', 'error');
        }
        await loadProducts();
        if ($('#viewOrders')?.classList.contains('active')) loadMyOrders();
        showView('orders');
    } catch (e) {
        toast(e.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Ödemeyi Onayla'; }
    }
}

function showView(name) {
    const map = { shop: 'viewShop', wishlist: 'viewWishlist', orders: 'viewOrders', admin: 'viewAdmin', product: 'viewProduct', order: 'viewOrder' };
    $$('.view').forEach((v) => v.classList.remove('active'));
    $(`#${map[name] || 'viewShop'}`)?.classList.add('active');
    $$('.nav-link[data-view]').forEach((n) => n.classList.toggle('active', n.dataset.view === name));
    $$('.mb-item[data-view]').forEach((n) => n.classList.toggle('active', n.dataset.view === name));
    if (name === 'orders') loadMyOrders();
    if (name === 'wishlist') loadWishlist();
    if (name === 'admin') loadAdmin();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateAuthUI() {
    const area = $('#authArea');
    const ordersTab = $('#ordersTab');
    const wishlistTab = $('#wishlistTab');
    const adminTab = $('#adminTab');
    if (!area) return;
    if (state.user) {
        area.innerHTML = `<div class="user-chip"><span class="role-badge">${state.user.role}</span><strong>${escapeHtml(state.user.fullName)}</strong><button class="btn btn-ghost btn-sm" id="profileBtn">Profil</button><button class="btn btn-ghost btn-sm" id="logoutBtn">Çıkış</button></div>`;
        $('#profileBtn').onclick = openProfile;
        $('#logoutBtn').onclick = logout;
        ordersTab.hidden = false;
        wishlistTab.hidden = false;
        adminTab.hidden = state.user.role !== 'ADMIN';
        $('#wishlistHeaderBtn').hidden = false;
        $('#mbWishlist').hidden = false;
        $('#mbOrders').hidden = false;
        loadAddresses();
        loadWishlistIds();
    } else {
        area.innerHTML = '<button class="btn btn-login" id="loginBtn">Giriş Yap</button>';
        $('#loginBtn').onclick = () => $('#authModal').showModal();
        ordersTab.hidden = true;
        wishlistTab.hidden = true;
        adminTab.hidden = true;
        $('#wishlistHeaderBtn').hidden = true;
    }
    renderMobileNav();
}

async function logout() {
    state.token = null;
    state.refreshToken = null;
    state.user = null;
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_REFRESH);
    localStorage.removeItem(STORAGE_USER);
    updateAuthUI();
    toast('Çıkış yapıldı');
}

async function loadCategories() {
    try {
        state.categories = await api('/api/categories');
        renderCategories();
        renderSidebarCats();
        renderCategoryChips();
    } catch (e) { console.warn('Categories:', e.message); }
}

function renderCategories() {
    const grid = $('#categoryGrid');
    if (!grid) return;
    grid.innerHTML = state.categories.slice(0, 8).map((c) =>
        `<button class="category-card" data-cat="${c.id}"><img src="https://picsum.photos/seed/cat-${c.slug}/400/280" alt=""><span>${escapeHtml(c.name)}</span></button>`
    ).join('');
    grid.querySelectorAll('[data-cat]').forEach((b) => {
        b.onclick = () => { state.categoryId = Number(b.dataset.cat); state.page = 0; showView('shop'); loadProducts(); document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' }); };
    });
}

function renderSidebarCats() {
    const ul = $('#sidebarCats');
    if (!ul) return;
    ul.innerHTML = `<li><button class="${!state.categoryId ? 'active' : ''}" data-cat="">Tümü</button></li>` +
        state.categories.map((c) => `<li><button class="${state.categoryId === c.id ? 'active' : ''}" data-cat="${c.id}">${escapeHtml(c.name)}</button></li>`).join('');
    ul.querySelectorAll('button').forEach((b) => {
        b.onclick = () => { state.categoryId = b.dataset.cat ? Number(b.dataset.cat) : null; state.page = 0; loadProducts(); };
    });
}

function renderCategoryChips() {
    const el = $('#categoryChips');
    if (!el) return;
    el.innerHTML = state.categories.map((c) =>
        `<button class="chip ${state.categoryId === c.id ? 'active' : ''}" data-cat="${c.id}">${escapeHtml(c.name)}</button>`
    ).join('');
    el.querySelectorAll('.chip').forEach((b) => {
        b.onclick = () => { state.categoryId = Number(b.dataset.cat); state.page = 0; loadProducts(); };
    });
}

async function loadProducts() {
    const grid = $('#productsGrid');
    if (grid) grid.innerHTML = '<p class="empty-state">Yükleniyor...</p>';
    const params = new URLSearchParams({ page: state.page, size: 12, sort: SORT_MAP[state.sort] || SORT_MAP.newest });
    if (state.search) params.set('q', state.search);
    if (state.categoryId) params.set('categoryId', state.categoryId);
    if (state.minPrice != null) params.set('minPrice', state.minPrice);
    if (state.maxPrice != null) params.set('maxPrice', state.maxPrice);
    try {
        const data = await api(`/api/products?${params}`);
        state.products = data.content || [];
        state.totalPages = data.totalPages || 0;
        renderProducts();
        renderPagination();
        $('#productCount').textContent = `${data.totalElements ?? state.products.length} ürün`;
        $('#shopTitle').textContent = state.categoryId
            ? (state.categories.find((c) => c.id === state.categoryId)?.name || 'Ürünler')
            : (state.search ? `"${state.search}" araması` : 'Tüm Ürünler');
    } catch (e) {
        if (grid) grid.innerHTML = `<div class="empty-state"><p>${escapeHtml(e.message)}</p></div>`;
    }
}

function renderProducts() {
    const grid = $('#productsGrid');
    if (!grid) return;
    if (!state.products.length) {
        grid.innerHTML = '<p class="empty-state">Ürün bulunamadı.</p>';
        return;
    }
    grid.innerHTML = state.products.map((p) => renderProductCard(p)).join('');
    bindProductCards(grid);
}

function renderProductCard(p) {
    const inWish = state.wishlist.includes(p.id);
    return `<article class="product-card" data-product="${p.id}">
        <div class="product-img-wrap"><img src="${getProductImage(p)}" alt="${escapeHtml(p.name)}" loading="lazy"></div>
        <div class="product-body">
            <span class="product-cat">${escapeHtml(p.categoryName || '')}</span>
            <h3>${escapeHtml(p.name)}</h3>
            <div class="product-rating">${renderStars(p.averageRating)} <small>(${p.reviewCount || 0})</small></div>
            <div class="product-price">${formatMoney(p.price)}</div>
            <div class="product-actions">
                <button class="btn btn-primary btn-sm add-cart" data-id="${p.id}">Sepete Ekle</button>
                <button class="btn btn-icon wish-btn ${inWish ? 'active' : ''}" data-wish="${p.id}" title="Favori">♥</button>
            </div>
        </div>
    </article>`;
}

function bindProductCards(container) {
    container.querySelectorAll('[data-product]').forEach((card) => {
        card.onclick = (e) => {
            if (e.target.closest('button')) return;
            showProductPage(Number(card.dataset.product));
        };
    });
    container.querySelectorAll('.add-cart').forEach((btn) => {
        btn.onclick = (e) => { e.stopPropagation(); addToCart(Number(btn.dataset.id)); };
    });
    container.querySelectorAll('.wish-btn').forEach((btn) => {
        btn.onclick = (e) => { e.stopPropagation(); toggleWishlist(Number(btn.dataset.wish)); };
    });
}

async function loadFeatured() {
    try {
        const items = await api('/api/products/featured?size=8');
        const grid = $('#featuredGrid');
        if (grid) {
            grid.innerHTML = items.length ? items.map(renderProductCard).join('') : '<p class="empty-state compact">Öne çıkan ürün yok</p>';
            bindProductCards(grid);
        }
    } catch (e) {
        if ($('#featuredGrid')) $('#featuredGrid').innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`;
    }
}

async function loadBestsellers() {
    try {
        const items = await api('/api/products/bestsellers?size=8');
        const grid = $('#bestsellersGrid');
        if (grid) {
            grid.innerHTML = items.length ? items.map(renderProductCard).join('') : '<p class="empty-state compact">Henüz veri yok</p>';
            bindProductCards(grid);
        }
    } catch (e) {
        if ($('#bestsellersGrid')) $('#bestsellersGrid').innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`;
    }
}

function renderPagination() {
    const el = $('#pagination');
    if (!el) return;
    if (state.totalPages <= 1) { el.hidden = true; return; }
    el.hidden = false;
    let html = '';
    for (let i = 0; i < state.totalPages; i++) {
        html += `<button class="page-btn ${i === state.page ? 'active' : ''}" data-page="${i}">${i + 1}</button>`;
    }
    el.innerHTML = html;
    el.querySelectorAll('[data-page]').forEach((b) => {
        b.onclick = () => { state.page = Number(b.dataset.page); loadProducts(); };
    });
}

async function showProductPage(productId) {
    state.selectedProductId = productId;
    let p = state.products.find((x) => x.id === productId);
    if (!p) {
        try { p = await api(`/api/products/${productId}`); } catch { toast('Ürün bulunamadı', 'error'); return; }
    }
    let reviews = [];
    try { reviews = await api(`/api/products/${productId}/reviews`); } catch { /* optional */ }
    const gallery = getProductGallery(p);
    const thumbs = gallery.map((url, i) =>
        `<button type="button" class="gallery-thumb ${i === 0 ? 'active' : ''}" data-img="${escapeHtml(url)}"><img src="${escapeHtml(url)}" alt=""></button>`
    ).join('');
    $('#productPageContent').innerHTML = `
        <div class="product-page-layout">
            <div class="product-gallery">
                <img class="product-page-img" id="productMainImg" src="${escapeHtml(gallery[0])}" alt="">
                <div class="gallery-thumbs">${thumbs}</div>
            </div>
            <div class="product-page-info">
                <span class="product-cat">${escapeHtml(p.categoryName || '')}</span>
                <h1>${escapeHtml(p.name)}</h1>
                <div class="product-rating">${renderStars(p.averageRating)} <small>(${p.reviewCount || 0} yorum)</small></div>
                <p class="product-page-price">${formatMoney(p.price)}</p>
                <p class="product-desc">${escapeHtml(p.description || '')}</p>
                <p class="product-stock ${(p.stockQuantity ?? p.stock) <= 5 ? 'low' : ''}">Stok: ${p.stockQuantity ?? p.stock ?? 0}</p>
                <div class="product-page-actions">
                    <button class="btn btn-primary btn-lg" id="pageAddCart">Sepete Ekle</button>
                    <button class="btn btn-outline" id="pageWishlist">${state.wishlist.includes(p.id) ? '♥ Favoride' : '♡ Favorile'}</button>
                    ${(p.stockQuantity ?? p.stock) === 0 ? `<button class="btn btn-outline" id="stockAlertBtn">Stok gelince haber ver</button>` : ''}
                </div>
            </div>
        </div>
        <section class="reviews-section">
            <h3>Değerlendirmeler</h3>
            <div class="reviews-list">${reviews.length ? reviews.map((r) =>
                `<div class="review-card"><strong>${escapeHtml(r.userName)}</strong> ${renderStars(r.rating)}<p>${escapeHtml(r.comment || '')}</p><time>${new Date(r.createdAt).toLocaleDateString('tr-TR')}</time></div>`
            ).join('') : '<p class="empty-state compact">Henüz yorum yok.</p>'}</div>
            ${state.token ? `<form id="reviewForm" class="review-form">
                <h4>Yorum yaz</h4>
                <label>Puan<select name="rating" required><option value="5">5</option><option value="4">4</option><option value="3">3</option><option value="2">2</option><option value="1">1</option></select></label>
                <label>Yorum<textarea name="comment" rows="2" placeholder="Ürün hakkında düşüncelerin..."></textarea></label>
                <button type="submit" class="btn btn-primary btn-sm">Gönder</button>
            </form>` : '<p class="sidebar-hint">Yorum yazmak için giriş yapın.</p>'}
        </section>`;
    $('#productPageContent').querySelectorAll('.gallery-thumb').forEach((btn) => {
        btn.onclick = () => {
            $('#productMainImg').src = btn.dataset.img;
            $('#productPageContent').querySelectorAll('.gallery-thumb').forEach((b) => b.classList.toggle('active', b === btn));
        };
    });
    $('#pageAddCart').onclick = () => addToCart(p.id);
    $('#pageWishlist')?.addEventListener('click', () => toggleWishlist(p.id));
    $('#stockAlertBtn')?.addEventListener('click', async () => {
        if (!state.token) { $('#authModal').showModal(); return; }
        try {
            await api(`/api/products/${p.id}/stock-alerts`, { method: 'POST' });
            toast('Stok gelince e-posta ile bilgilendirileceksin');
        } catch (e) { toast(e.message, 'error'); }
    });
    $('#reviewForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            await api(`/api/products/${p.id}/reviews`, {
                method: 'POST',
                body: JSON.stringify({ rating: Number(fd.get('rating')), comment: fd.get('comment') || '' }),
            });
            toast('Yorumun eklendi');
            showProductPage(p.id);
        } catch (err) { toast(err.message, 'error'); }
    });
    showView('product');
    loadSimilarProducts(p);
}

async function loadSimilarProducts(p) {
    const section = $('#similarProducts');
    const grid = $('#similarGrid');
    if (!section || !grid) return;
    try {
        const params = new URLSearchParams({ categoryId: p.categoryId, size: 4, page: 0 });
        const data = await api(`/api/products?${params}`);
        const similar = (data.content || []).filter((x) => x.id !== p.id).slice(0, 4);
        if (!similar.length) { section.hidden = true; return; }
        section.hidden = false;
        grid.innerHTML = similar.map(renderProductCard).join('');
        bindProductCards(grid);
    } catch { section.hidden = true; }
}

function addToCart(productId) {
    const p = state.products.find((x) => x.id === productId);
    const existing = state.cart.find((i) => i.productId === productId);
    if (existing) existing.quantity += 1;
    else state.cart.push({ productId, name: p?.name || `Ürün #${productId}`, price: p?.price || 0, quantity: 1, imageUrl: p?.imageUrl });
    saveCart();
    toast('Sepete eklendi');
    if (state.token) syncBackendCart().catch(() => {});
}

function updateCartUI() {
    const items = $('#cartItems');
    const count = state.cart.reduce((s, i) => s + i.quantity, 0);
    $('#cartBadge').textContent = count;
    $('#cartItemCount').textContent = count;
    if (!items) return;
    if (!state.cart.length) {
        items.innerHTML = '<p class="empty-state">Sepetin boş</p>';
    } else {
        items.innerHTML = state.cart.map((i) => {
            const img = i.imageUrl || `https://picsum.photos/seed/cart-${i.productId}/128/128`;
            const lineTotal = formatMoney(Number(i.price) * i.quantity);
            return `<div class="cart-line">
                <img class="cart-line-thumb" src="${escapeHtml(img)}" alt="">
                <div class="cart-line-info">
                    <h4>${escapeHtml(i.name)}</h4>
                    <div class="cart-line-qty">
                        <button type="button" class="qty-minus" data-id="${i.productId}" aria-label="Azalt">−</button>
                        <span>${i.quantity}</span>
                        <button type="button" class="qty-plus" data-id="${i.productId}" aria-label="Artır">+</button>
                    </div>
                </div>
                <div class="cart-line-price">${lineTotal}</div>
                <button type="button" class="btn-icon remove-item" data-id="${i.productId}" aria-label="Kaldır">×</button>
            </div>`;
        }).join('');
        items.querySelectorAll('.qty-minus').forEach((b) => b.onclick = () => changeQty(Number(b.dataset.id), -1));
        items.querySelectorAll('.qty-plus').forEach((b) => b.onclick = () => changeQty(Number(b.dataset.id), 1));
        items.querySelectorAll('.remove-item').forEach((b) => b.onclick = () => removeFromCart(Number(b.dataset.id)));
    }
    const subtotal = state.cart.reduce((s, i) => s + Number(i.price) * i.quantity, 0);
    const discount = state.appliedCoupon?.valid ? subtotal * (state.appliedCoupon.discountPercent / 100) : 0;
    $('#cartSubtotal').textContent = formatMoney(subtotal);
    $('#cartDiscountRow').hidden = !discount;
    $('#cartDiscount').textContent = `-${formatMoney(discount)}`;
    $('#cartTotal').textContent = formatMoney(subtotal - discount);
    const bar = $('#cartProgressBar');
    if (bar) bar.style.setProperty('--progress', `${Math.min(100, (subtotal / 500) * 100)}%`);
    const progressText = $('#cartProgressText');
    if (progressText) {
        progressText.textContent = subtotal >= 500
            ? '🎉 Ücretsiz kargo kazandın!'
            : `${formatMoney(500 - subtotal)} daha ekle, kargo bedava!`;
    }
}

function changeQty(id, d) {
    const item = state.cart.find((i) => i.productId === id);
    if (!item) return;
    item.quantity += d;
    if (item.quantity <= 0) state.cart = state.cart.filter((i) => i.productId !== id);
    saveCart();
}
function removeFromCart(id) {
    state.cart = state.cart.filter((i) => i.productId !== id);
    saveCart();
}
function openCart() { $('#cartDrawer')?.classList.add('open'); $('#overlay')?.classList.add('visible'); }
function closeCart() { $('#cartDrawer')?.classList.remove('open'); if (!$('#mobileNavDrawer')?.classList.contains('open')) $('#overlay')?.classList.remove('visible'); }

async function syncBackendCart() {
    for (const item of state.cart) {
        await api('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId: item.productId, quantity: item.quantity }) });
    }
}

async function loadAddresses() {
    if (!state.token) { state.addresses = []; renderAddressSelect(); return; }
    try {
        state.addresses = await api('/api/addresses');
        const def = state.addresses.find((a) => a.defaultAddress);
        state.selectedAddressId = def?.id || state.addresses[0]?.id;
        renderAddressSelect();
    } catch { state.addresses = []; }
}
function renderAddressSelect() {
    const sel = $('#checkoutAddress');
    if (!sel) return;
    if (!state.addresses.length) {
        sel.innerHTML = '<option value="">Henüz adres yok — ekleyin</option>';
        return;
    }
    sel.innerHTML = state.addresses.map((a) =>
        `<option value="${a.id}" ${a.id === state.selectedAddressId ? 'selected' : ''}>${escapeHtml(a.title)} — ${escapeHtml(a.district)}</option>`
    ).join('');
}

async function checkout() {
    if (!state.token) { toast('Önce giriş yapmalısın', 'error'); $('#authModal').showModal(); return; }
    if (!state.cart.length) { toast('Sepet boş', 'error'); return; }
    const addressId = Number($('#checkoutAddress')?.value) || state.selectedAddressId;
    if (!addressId) { toast('Teslimat adresi seçin', 'error'); $('#addressModal').showModal(); return; }
    const couponCode = state.appliedCoupon?.valid ? state.appliedCoupon.code : ($('#couponInput')?.value?.trim() || null);
    const btn = $('#checkoutBtn');
    const prev = btn?.textContent;
    if (btn) { btn.disabled = true; btn.textContent = 'Hazırlanıyor...'; }
    try {
        if (state.token) await syncBackendCart();
        const order = await api('/api/cart/checkout', {
            method: 'POST',
            body: JSON.stringify({ couponCode, addressId }),
        });
        state.cart = [];
        state.appliedCoupon = null;
        saveCart();
        openPaymentModal(order);
    } catch (e) {
        toast(e.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = prev || 'Ödemeye Geç →'; }
    }
}

async function loadMyOrders() {
    const el = $('#ordersList');
    if (!el) return;
    el.innerHTML = '<p class="empty-state">Yükleniyor...</p>';
    try {
        const orders = await api('/api/orders/me');
        el.innerHTML = orders.length ? orders.map(renderOrderCard).join('') : '<p class="empty-state">Henüz siparişin yok.</p>';
        bindOrderActions(el);
    } catch (e) {
        el.innerHTML = `<p class="empty-state">${escapeHtml(e.message)}</p>`;
    }
}

function renderOrderCard(o) {
    const items = o.items.map((i) => `<li>${escapeHtml(i.productName)} × ${i.quantity} — ${formatMoney(i.subtotal)}</li>`).join('');
    return `<div class="order-card">
        <div class="order-card-header">
            <div><strong>Sipariş #${o.id}</strong><div class="order-meta">${new Date(o.createdAt).toLocaleString('tr-TR')}</div></div>
            <span class="order-status ${o.status}">${o.status}</span>
        </div>
        ${renderOrderTimeline(o.status)}
        <ul class="order-items">${items}</ul>
        <div class="order-total">Toplam: ${formatMoney(o.totalAmount)}${o.discountAmount > 0 ? ` <small>(indirim -${formatMoney(o.discountAmount)})</small>` : ''}</div>
        ${o.trackingNumber ? `<div class="order-tracking">📦 Kargo: <strong>${escapeHtml(o.trackingNumber)}</strong></div>` : ''}
        <div class="order-card-actions">
            <button class="btn btn-outline btn-sm" data-order-detail="${o.id}">Detay</button>
            ${o.status === 'PENDING' ? `<button class="btn btn-primary btn-sm pay-order" data-pay="${o.id}">Öde</button>
            <button class="btn btn-outline btn-sm cancel-order" data-cancel="${o.id}">İptal</button>` : ''}
        </div></div>`;
}

async function openOrderDetail(orderId) {
    try {
        const o = await api(`/api/orders/${orderId}`);
        const items = o.items.map((i) =>
            `<li>${escapeHtml(i.productName)} × ${i.quantity} — ${formatMoney(i.subtotal)}</li>`
        ).join('');
        const ship = o.shippingAddress ? `
            <div class="order-shipping-box">
                <strong>Teslimat</strong>
                <p>${escapeHtml(o.shippingAddress.fullName)}<br>
                ${escapeHtml(o.shippingAddress.addressLine)}, ${escapeHtml(o.shippingAddress.district)} / ${escapeHtml(o.shippingAddress.city)}<br>
                Tel: ${escapeHtml(o.shippingAddress.phone)}</p>
            </div>` : '';
        $('#orderDetailContent').innerHTML = `
            <div class="order-detail-page">
                <div class="order-card-header">
                    <div><h1>Sipariş #${o.id}</h1><span class="order-status ${o.status}">${o.status}</span></div>
                    <time>${new Date(o.createdAt).toLocaleString('tr-TR')}</time>
                </div>
                ${renderOrderTimeline(o.status)}
                ${o.trackingNumber ? `<div class="order-tracking-box"><strong>Kargo takip</strong><code>${escapeHtml(o.trackingNumber)}</code></div>` : '<p class="sidebar-hint">Kargo takip no. sipariş kargoya verilince oluşur.</p>'}
                <ul class="order-items">${items}</ul>
                ${ship}
                <div class="order-total">Toplam: ${formatMoney(o.totalAmount)}</div>
            </div>`;
        showView('order');
    } catch (e) { toast(e.message, 'error'); }
}

function bindOrderActions(container) {
    container?.querySelectorAll('[data-order-detail]').forEach((btn) => {
        btn.onclick = () => openOrderDetail(Number(btn.dataset.orderDetail));
    });
    container?.querySelectorAll('.pay-order').forEach((btn) => {
        btn.onclick = async () => {
            try {
                const order = await api(`/api/orders/${btn.dataset.pay}`);
                openPaymentModal(order);
            } catch (e) { toast(e.message, 'error'); }
        };
    });
    container?.querySelectorAll('.cancel-order').forEach((btn) => {
        btn.onclick = async () => {
            if (!confirm('İptal edilsin mi?')) return;
            try {
                await api(`/api/orders/${btn.dataset.cancel}/cancel`, { method: 'POST' });
                toast('Sipariş iptal edildi');
                loadMyOrders();
            } catch (e) { toast(e.message, 'error'); }
        };
    });
}

async function openProfile() {
    if (!state.token) return;
    try {
        const p = await api('/api/users/me');
        $('#profileContent').innerHTML = `
            <div class="profile-card">
                <p><strong>Ad:</strong> ${escapeHtml(p.fullName)}</p>
                <p><strong>E-posta:</strong> ${escapeHtml(p.email)}</p>
                <p><strong>Rol:</strong> <span class="role-badge">${p.role}</span></p>
                <p><strong>Sipariş sayısı:</strong> ${p.orderCount}</p>
                <p><strong>Üyelik:</strong> ${new Date(p.createdAt).toLocaleDateString('tr-TR')}</p>
            </div>`;
        $('#profileModal').showModal();
    } catch (e) { toast(e.message, 'error'); }
}

async function loadWishlistIds() {
    if (!state.token) return;
    try {
        const list = await api('/api/wishlist');
        state.wishlist = list.map((w) => w.productId);
    } catch { state.wishlist = []; }
}
async function toggleWishlist(id) {
    if (!state.token) { toast('Giriş yapın', 'error'); return; }
    try {
        if (state.wishlist.includes(id)) {
            await api(`/api/wishlist/${id}`, { method: 'DELETE' });
            state.wishlist = state.wishlist.filter((x) => x !== id);
        } else {
            await api(`/api/wishlist/${id}`, { method: 'POST' });
            state.wishlist.push(id);
        }
        loadProducts();
    } catch (e) { toast(e.message, 'error'); }
}
async function loadWishlist() {
    const grid = $('#wishlistGrid');
    if (!grid) return;
    if (!state.token) { grid.innerHTML = '<p class="empty-state">Giriş yapın</p>'; return; }
    try {
        const list = await api('/api/wishlist');
        const products = await Promise.all(list.map((w) => api(`/api/products/${w.productId}`).catch(() => null)));
        grid.innerHTML = products.filter(Boolean).map(renderProductCard).join('') || '<p class="empty-state">Favori yok</p>';
        bindProductCards(grid);
    } catch (e) { grid.innerHTML = `<p class="empty-state">${escapeHtml(e.message)}</p>`; }
}

function renderAdminChart(chartData) {
    const canvas = $('#ordersChart');
    if (!canvas || typeof Chart === 'undefined') return;
    if (state.chart) { state.chart.destroy(); state.chart = null; }
    state.chart = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: chartData.labels,
            datasets: [
                { label: 'Sipariş', data: chartData.orderCounts, backgroundColor: 'rgba(242, 122, 26, 0.75)', borderRadius: 6 },
                { label: 'Gelir (₺)', data: chartData.revenues, type: 'line', borderColor: '#2d3a4a', backgroundColor: 'transparent', yAxisID: 'y1' },
            ],
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } },
            scales: { y: { beginAtZero: true }, y1: { position: 'right', beginAtZero: true } },
        },
    });
}

async function loadAdminStats() {
    const stats = await api('/api/admin/stats');
    $('#adminStats').innerHTML = Object.entries(stats).map(([k, v]) => {
        const label = ADMIN_STAT_LABELS[k] || k;
        const val = k === 'totalRevenue' ? formatMoney(v) : v;
        return `<div class="stat-card"><span>${label}</span><strong>${val}</strong></div>`;
    }).join('');
}

async function loadAdminChart() {
    try {
        const chartData = await api('/api/admin/charts/orders?months=6');
        renderAdminChart(chartData);
    } catch { /* optional */ }
}

async function loadStockAlerts() {
    const panel = $('#stockAlertsPanel');
    const el = $('#stockAlerts');
    if (!el) return;
    try {
        const products = await api('/api/admin/stock-alerts');
        if (!products.length) {
            panel.hidden = true;
            return;
        }
        panel.hidden = false;
        el.innerHTML = `<table class="admin-table"><thead><tr><th>Ürün</th><th>Stok</th></tr></thead><tbody>
            ${products.map((p) => `<tr><td>${escapeHtml(p.name)}</td><td class="text-danger"><strong>${p.stockQuantity}</strong></td></tr>`).join('')}
        </tbody></table>`;
    } catch { panel.hidden = true; }
}

async function loadAdminAuditLogs() {
    const el = $('#auditLogs');
    if (!el) return;
    try {
        const logs = await api('/api/admin/audit-logs');
        el.innerHTML = logs.length ? logs.map((l) =>
            `<div class="audit-item"><span class="audit-action">${escapeHtml(l.action)}</span>
            <span>${escapeHtml(l.userEmail)} · ${escapeHtml(l.entityType)} #${l.entityId || '—'}</span>
            <time>${new Date(l.createdAt).toLocaleString('tr-TR')}</time></div>`
        ).join('') : '<p class="empty-state compact">Kayıt yok</p>';
    } catch (e) { el.innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`; }
}

async function loadAdminCategories() {
    const el = $('#adminCategories');
    if (!el) return;
    try {
        const cats = await api('/api/categories');
        el.innerHTML = `<table class="admin-table"><thead><tr><th>Ad</th><th>Slug</th><th></th></tr></thead><tbody>
            ${cats.map((c) => `<tr>
                <td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.slug)}</td>
                <td class="admin-actions">
                    <button class="btn btn-ghost btn-sm" data-edit-cat="${c.id}">Düzenle</button>
                    <button class="btn btn-ghost btn-sm text-danger" data-del-cat="${c.id}">Sil</button>
                </td></tr>`).join('')}
        </tbody></table>`;
        el.querySelectorAll('[data-edit-cat]').forEach((btn) => {
            btn.onclick = async () => {
                const cats = await api('/api/categories');
                const c = cats.find((x) => x.id === Number(btn.dataset.editCat));
                if (c) openCategoryModal(c);
            };
        });
        el.querySelectorAll('[data-del-cat]').forEach((btn) => {
            btn.onclick = async () => {
                if (!confirm('Kategori silinsin mi?')) return;
                try {
                    await api(`/api/categories/${btn.dataset.delCat}`, { method: 'DELETE' });
                    toast('Kategori silindi');
                    loadAdminCategories();
                    loadCategories();
                } catch (e) { toast(e.message, 'error'); }
            };
        });
    } catch (e) { el.innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`; }
}

async function loadAdminProducts() {
    const el = $('#adminProducts');
    if (!el) return;
    try {
        const data = await api('/api/products?size=50&page=0');
        const products = data.content || [];
        el.innerHTML = `<table class="admin-table"><thead><tr><th>Ürün</th><th>Fiyat</th><th>Stok</th><th></th></tr></thead><tbody>
            ${products.map((p) => `<tr>
                <td>${escapeHtml(p.name)}</td>
                <td>${formatMoney(p.price)}</td>
                <td>${p.stockQuantity}</td>
                <td class="admin-actions">
                    <button class="btn btn-ghost btn-sm" data-edit-prod="${p.id}">Düzenle</button>
                    <button class="btn btn-ghost btn-sm text-danger" data-del-prod="${p.id}">Sil</button>
                </td></tr>`).join('')}
        </tbody></table>`;
        el.querySelectorAll('[data-edit-prod]').forEach((btn) => {
            btn.onclick = async () => {
                const p = await api(`/api/products/${btn.dataset.editProd}`);
                openProductModal(p);
            };
        });
        el.querySelectorAll('[data-del-prod]').forEach((btn) => {
            btn.onclick = async () => {
                if (!confirm('Ürün silinsin mi?')) return;
                try {
                    await api(`/api/products/${btn.dataset.delProd}`, { method: 'DELETE' });
                    toast('Ürün silindi');
                    loadAdminProducts();
                    loadProducts();
                } catch (e) { toast(e.message, 'error'); }
            };
        });
    } catch (e) { el.innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`; }
}

async function loadAdminOrders() {
    const el = $('#adminOrders');
    if (!el) return;
    try {
        const orders = await api('/api/admin/orders');
        el.innerHTML = `<table class="admin-table"><thead><tr><th>#</th><th>Müşteri</th><th>Durum</th><th>Toplam</th><th></th></tr></thead><tbody>
            ${orders.map((o) => `<tr>
                <td>${o.id}</td>
                <td>${escapeHtml(o.userEmail || '—')}</td>
                <td><select class="admin-status-select" data-order-id="${o.id}">
                    ${ORDER_STATUSES.map((s) => `<option value="${s}" ${o.status === s ? 'selected' : ''}>${s}</option>`).join('')}
                </select></td>
                <td>${formatMoney(o.totalAmount)}</td>
                <td><button class="btn btn-ghost btn-sm" data-save-status="${o.id}">Kaydet</button></td>
            </tr>`).join('')}
        </tbody></table>`;
        el.querySelectorAll('[data-save-status]').forEach((btn) => {
            btn.onclick = async () => {
                const id = btn.dataset.saveStatus;
                const sel = el.querySelector(`select[data-order-id="${id}"]`);
                try {
                    await api(`/api/admin/orders/${id}/status`, {
                        method: 'PATCH',
                        body: JSON.stringify({ status: sel.value }),
                    });
                    toast('Durum güncellendi');
                    loadAdminOrders();
                } catch (e) { toast(e.message, 'error'); }
            };
        });
    } catch (e) { el.innerHTML = `<p class="empty-state compact">${escapeHtml(e.message)}</p>`; }
}

function fillCategorySelect() {
    const sel = $('#productCategorySelect');
    if (!sel) return;
    sel.innerHTML = state.categories.map((c) =>
        `<option value="${c.id}">${escapeHtml(c.name)}</option>`
    ).join('');
}

function openCategoryModal(cat = null) {
    const form = $('#categoryForm');
    if (!form) return;
    form.reset();
    form.elements.id.value = cat?.id || '';
    form.elements.name.value = cat?.name || '';
    form.elements.slug.value = cat?.slug || '';
    $('#categoryModalTitle').textContent = cat ? 'Kategori Düzenle' : 'Yeni Kategori';
    $('#categoryModal').showModal();
}

function openProductModal(product = null) {
    fillCategorySelect();
    const form = $('#productForm');
    if (!form) return;
    form.reset();
    form.elements.id.value = product?.id || '';
    form.elements.name.value = product?.name || '';
    form.elements.description.value = product?.description || '';
    form.elements.price.value = product?.price || '';
    form.elements.stockQuantity.value = product?.stockQuantity ?? 10;
    if (product?.categoryId) form.elements.categoryId.value = product.categoryId;
    $('#productModalTitle').textContent = product ? 'Ürün Düzenle' : 'Yeni Ürün';
    $('#productImageField').classList.toggle('hidden', !product?.id);
    $('#productModal').showModal();
}

async function loadAdmin() {
    if (state.user?.role !== 'ADMIN') return;
    try {
        await Promise.all([
            loadAdminStats(),
            loadAdminChart(),
            loadStockAlerts(),
            loadAdminAuditLogs(),
            loadAdminCategories(),
            loadAdminProducts(),
            loadAdminOrders(),
        ]);
    } catch (e) { toast(e.message, 'error'); }
}

function renderMobileNav() {
    const nav = $('#mobileNavLinks');
    if (!nav) return;
    nav.innerHTML = `
        <button data-view="shop">Ana Sayfa</button>
        <button data-view="wishlist" ${!state.token ? 'hidden' : ''}>Favoriler</button>
        <button data-view="orders" ${!state.token ? 'hidden' : ''}>Siparişler</button>
        ${state.user?.role === 'ADMIN' ? '<button data-view="admin">Yönetim</button>' : ''}`;
    nav.querySelectorAll('button').forEach((b) => b.onclick = () => { showView(b.dataset.view); closeMobileNav(); });
}

function closeMobileNav() {
    $('#mobileNavDrawer')?.classList.remove('open');
    $('#overlay')?.classList.remove('visible');
}

function initHero() {
    const slides = $$('.hero-slide');
    const dots = $('#heroDots');
    if (!slides.length) return;
    dots.innerHTML = [...slides].map((_, i) => `<button class="${i === 0 ? 'active' : ''}" data-slide="${i}"></button>`).join('');
    dots.querySelectorAll('button').forEach((b) => b.onclick = () => goHero(Number(b.dataset.slide)));
    $('#heroPrev')?.addEventListener('click', () => goHero(state.heroSlide - 1));
    $('#heroNext')?.addEventListener('click', () => goHero(state.heroSlide + 1));
    clearInterval(state.heroTimer);
    state.heroTimer = setInterval(() => goHero(state.heroSlide + 1), 6000);
}
function goHero(i) {
    const slides = $$('.hero-slide');
    state.heroSlide = ((i % slides.length) + slides.length) % slides.length;
    slides.forEach((s, idx) => s.classList.toggle('active', idx === state.heroSlide));
    $$('#heroDots button').forEach((b, idx) => b.classList.toggle('active', idx === state.heroSlide));
}

function bindEvents() {
    $('#searchBtn')?.addEventListener('click', () => { state.search = $('#searchInput')?.value?.trim() || ''; state.page = 0; loadProducts(); });
    $('#searchInput')?.addEventListener('keydown', (e) => { if (e.key === 'Enter') $('#searchBtn')?.click(); });
    $('#sortSelect')?.addEventListener('change', (e) => { state.sort = e.target.value; state.page = 0; loadProducts(); });
    $('#applyPriceFilter')?.addEventListener('click', () => {
        state.minPrice = $('#minPrice')?.value ? Number($('#minPrice').value) : null;
        state.maxPrice = $('#maxPrice')?.value ? Number($('#maxPrice').value) : null;
        state.page = 0; loadProducts();
    });
    $('#clearPriceFilter')?.addEventListener('click', () => {
        state.minPrice = state.maxPrice = null;
        if ($('#minPrice')) $('#minPrice').value = '';
        if ($('#maxPrice')) $('#maxPrice').value = '';
        state.page = 0; loadProducts();
    });
    $('#cartBtn')?.addEventListener('click', openCart);
    $('#closeCart')?.addEventListener('click', closeCart);
    $('#checkoutBtn')?.addEventListener('click', checkout);
    $('#overlay')?.addEventListener('click', () => { closeCart(); closeMobileNav(); });
    $$('.nav-link[data-view]').forEach((b) => b.addEventListener('click', () => showView(b.dataset.view)));
    $$('.mb-item[data-view]').forEach((b) => b.addEventListener('click', () => showView(b.dataset.view)));
    $('#mbCart')?.addEventListener('click', openCart);
    $('#mbAccount')?.addEventListener('click', () => state.token ? showView('orders') : $('#authModal').showModal());
    $('#mobileMenuBtn')?.addEventListener('click', () => { $('#mobileNavDrawer')?.classList.add('open'); $('#overlay')?.classList.add('visible'); });
    $('#closeMobileNav')?.addEventListener('click', closeMobileNav);
    $('#loginBtn')?.addEventListener('click', () => $('#authModal').showModal());
    $('#themeToggle')?.addEventListener('click', () => {
        document.body.classList.toggle('dark-theme');
        localStorage.setItem('shopflow_theme', document.body.classList.contains('dark-theme') ? 'dark' : 'light');
        $('#themeToggle').textContent = document.body.classList.contains('dark-theme') ? '☀️' : '🌙';
    });
    if (localStorage.getItem('shopflow_theme') === 'dark') {
        document.body.classList.add('dark-theme');
        if ($('#themeToggle')) $('#themeToggle').textContent = '☀️';
    }

    $('#fillSuccessCard')?.addEventListener('click', () => fillDemoCard(true));
    $('#fillFailCard')?.addEventListener('click', () => fillDemoCard(false));

    $('#cardNumber')?.addEventListener('input', (e) => { e.target.value = formatCardNumber(e.target.value); updateCardPreview(); });
    $('#cardExpiry')?.addEventListener('input', (e) => { e.target.value = formatCardExpiry(e.target.value); updateCardPreview(); });
    $('#cardName')?.addEventListener('input', updateCardPreview);
    $('#paymentForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        if (state.pendingPaymentOrder?.id) processPayment(state.pendingPaymentOrder.id);
    });
    $('#closePayment')?.addEventListener('click', () => {
        $('#paymentModal')?.close();
        if (state.pendingPaymentOrder) toast('Sipariş oluşturuldu — ödemeyi Siparişlerimden tamamlayabilirsin.');
        state.pendingPaymentOrder = null;
    });

    $('#loginForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            const data = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: fd.get('email'), password: fd.get('password') }) });
            state.token = data.token;
            state.refreshToken = data.refreshToken;
            state.user = { email: data.email, fullName: data.fullName, role: data.role };
            localStorage.setItem(STORAGE_TOKEN, state.token);
            if (data.refreshToken) localStorage.setItem(STORAGE_REFRESH, data.refreshToken);
            localStorage.setItem(STORAGE_USER, JSON.stringify(state.user));
            $('#authModal').close();
            updateAuthUI();
            toast('Hoş geldin!');
        } catch (err) { toast(err.message, 'error'); }
    });

    $('#registerForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            const data = await api('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ email: fd.get('email'), password: fd.get('password'), fullName: fd.get('fullName') }),
            });
            state.token = data.token;
            state.refreshToken = data.refreshToken;
            state.user = { email: data.email, fullName: data.fullName, role: data.role };
            localStorage.setItem(STORAGE_TOKEN, state.token);
            if (data.refreshToken) localStorage.setItem(STORAGE_REFRESH, data.refreshToken);
            localStorage.setItem(STORAGE_USER, JSON.stringify(state.user));
            $('#authModal').close();
            updateAuthUI();
            toast('Kayıt başarılı!');
        } catch (err) { toast(err.message, 'error'); }
    });

    $$('.auth-tab').forEach((tab) => tab.addEventListener('click', () => {
        $$('.auth-tab').forEach((t) => t.classList.toggle('active', t === tab));
        $('#loginForm').hidden = tab.dataset.auth !== 'login';
        $('#registerForm').hidden = tab.dataset.auth !== 'register';
    }));

    $('#applyCouponBtn')?.addEventListener('click', async () => {
        const code = $('#couponInput')?.value?.trim();
        const subtotal = state.cart.reduce((s, i) => s + Number(i.price) * i.quantity, 0);
        if (!code) return;
        try {
            const res = await api('/api/coupons/validate', { method: 'POST', body: JSON.stringify({ code, orderAmount: subtotal }) });
            state.appliedCoupon = res.valid ? { ...res, code } : null;
            $('#couponHint').textContent = res.valid ? `%${res.discountPercent} indirim uygulandı` : res.message;
            updateCartUI();
        } catch (e) { $('#couponHint').textContent = e.message; }
    });

    $('#addressForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const body = Object.fromEntries(fd.entries());
        body.defaultAddress = !!fd.get('defaultAddress');
        try {
            await api('/api/addresses', { method: 'POST', body: JSON.stringify(body) });
            $('#addressModal').close();
            await loadAddresses();
            toast('Adres kaydedildi');
        } catch (err) { toast(err.message, 'error'); }
    });
    $('#manageAddressBtn')?.addEventListener('click', () => $('#addressModal').showModal());
    $('#closeAddress')?.addEventListener('click', () => $('#addressModal').close());
    $('#backFromProduct')?.addEventListener('click', () => showView('shop'));
    $('#backFromOrder')?.addEventListener('click', () => showView('orders'));
    $('#closeProfile')?.addEventListener('click', () => $('#profileModal')?.close());
    $('#closeProduct')?.addEventListener('click', () => $('#productModal')?.close());
    $('#closeCategory')?.addEventListener('click', () => $('#categoryModal')?.close());

    $('#newCategoryBtn')?.addEventListener('click', () => openCategoryModal());
    $('#newProductBtn')?.addEventListener('click', () => openProductModal());

    $('#categoryForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const id = fd.get('id');
        const body = { name: fd.get('name'), slug: fd.get('slug') };
        try {
            if (id) await api(`/api/categories/${id}`, { method: 'PUT', body: JSON.stringify(body) });
            else await api('/api/categories', { method: 'POST', body: JSON.stringify(body) });
            $('#categoryModal').close();
            toast('Kategori kaydedildi');
            await loadCategories();
            loadAdminCategories();
        } catch (err) { toast(err.message, 'error'); }
    });

    $('#productForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const id = fd.get('id');
        const body = {
            name: fd.get('name'),
            description: fd.get('description') || '',
            price: Number(fd.get('price')),
            stockQuantity: Number(fd.get('stockQuantity')),
            categoryId: Number(fd.get('categoryId')),
            lowStockThreshold: 5,
        };
        try {
            let product;
            if (id) product = await api(`/api/products/${id}`, { method: 'PUT', body: JSON.stringify(body) });
            else product = await api('/api/products', { method: 'POST', body: JSON.stringify(body) });
            const file = $('#productImageInput')?.files?.[0];
            if (file && product?.id) {
                const imgFd = new FormData();
                imgFd.append('file', file);
                const headers = { Authorization: `Bearer ${state.token}` };
                const res = await fetch(`${API}/api/products/${product.id}/image`, { method: 'POST', headers, body: imgFd });
                if (!res.ok) throw new Error('Görsel yüklenemedi');
            }
            $('#productModal').close();
            toast('Ürün kaydedildi');
            loadAdminProducts();
            loadProducts();
        } catch (err) { toast(err.message, 'error'); }
    });

    $('#forgotPasswordLink')?.addEventListener('click', (e) => {
        e.preventDefault();
        $('#authModal').close();
        $('#forgotPasswordModal').showModal();
    });
    $('#closeForgotPassword')?.addEventListener('click', () => $('#forgotPasswordModal')?.close());
    $('#forgotPasswordForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = new FormData(e.target).get('email');
        try {
            await api('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) });
            toast('Sıfırlama bağlantısı gönderildi (demo: mail log)');
            $('#forgotPasswordModal').close();
        } catch (err) { toast(err.message, 'error'); }
    });

    $('#applyPriceFilterMobile')?.addEventListener('click', () => {
        state.minPrice = $('#minPriceMobile')?.value ? Number($('#minPriceMobile').value) : null;
        state.maxPrice = $('#maxPriceMobile')?.value ? Number($('#maxPriceMobile').value) : null;
        state.page = 0;
        loadProducts();
    });

    document.querySelectorAll('.footer-links a[data-view]').forEach((a) => {
        a.addEventListener('click', (e) => {
            e.preventDefault();
            if (a.dataset.view === 'orders' && !state.token) {
                toast('Siparişler için giriş yapın', 'error');
                $('#authModal')?.showModal();
                return;
            }
            showView(a.dataset.view);
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    });

    document.querySelector('.footer-newsletter-form')?.addEventListener('submit', (e) => {
        e.preventDefault();
        const input = e.target.querySelector('input[type="email"]');
        if (!input?.value?.trim()) {
            toast('E-posta adresi girin', 'error');
            return;
        }
        toast('Abonelik kaydedildi! (demo)');
        input.value = '';
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    bindEvents();
    updateAuthUI();
    updateCartUI();
    initHero();
    await loadCategories();
    await Promise.all([loadProducts(), loadFeatured(), loadBestsellers()]);
});

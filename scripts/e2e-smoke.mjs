/**
 * ShopFlow E2E smoke tests (Playwright, no test runner).
 * Requires app at http://localhost:8080
 */
import { chromium } from 'playwright';

const base = process.env.E2E_BASE_URL || 'http://localhost:8080';
let failed = 0;

function assert(condition, message) {
    if (!condition) {
        console.error('FAIL:', message);
        failed++;
    } else {
        console.log('OK:', message);
    }
}

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

try {
    await page.goto(base, { waitUntil: 'networkidle', timeout: 120000 });
    assert((await page.title())?.length > 0, 'Home page loads');

    const productCount = await page.locator('.product-card').count();
    assert(productCount > 0, `Products visible (${productCount})`);

    const cfgRes = await page.request.get(`${base}/api/payments/config`);
    assert(cfgRes.ok(), 'Payment config API');
    const cfg = await cfgRes.json();
    assert(typeof cfg.stripeEnabled === 'boolean', 'Stripe config shape');

    const email = `e2e-${Date.now()}@test.com`;
    const reg = await page.request.post(`${base}/api/auth/register`, {
        data: { email, password: 'password123', fullName: 'E2E User' },
    });
    assert(reg.status() === 201, 'Register API');

    const login = await page.request.post(`${base}/api/auth/login`, {
        data: { email, password: 'password123' },
    });
    assert(login.ok(), 'Login API');
    const { token } = await login.json();
    const headers = { Authorization: `Bearer ${token}` };

    const products = await page.request.get(`${base}/api/products?size=1`);
    const productId = (await products.json()).content[0].id;

    await page.request.post(`${base}/api/cart/items`, {
        headers,
        data: { productId, quantity: 1 },
    });

    await page.request.post(`${base}/api/addresses`, {
        headers,
        data: {
            title: 'Ev', fullName: 'E2E', phone: '05551112233',
            city: 'Istanbul', district: 'Kadikoy', addressLine: 'Test 1',
            postalCode: '34710', defaultAddress: true,
        },
    });
    const addresses = await page.request.get(`${base}/api/addresses`, { headers });
    const addressId = (await addresses.json())[0].id;

    const checkout = await page.request.post(`${base}/api/cart/checkout`, {
        headers,
        data: { addressId },
    });
    assert(checkout.status() === 201, 'Checkout API');
    const orderId = (await checkout.json()).id;

    const pay = await page.request.post(`${base}/api/orders/${orderId}/pay`, {
        headers,
        data: { idempotencyKey: `e2e-${orderId}`, simulateFailure: false },
    });
    assert(pay.ok(), 'Simulated payment API');
    const payBody = await pay.json();
    assert(payBody.status === 'PAID', 'Payment status PAID');

    await page.goto(`${base}/swagger-ui.html`, { waitUntil: 'domcontentloaded', timeout: 60000 });
    assert(page.url().includes('swagger'), 'Swagger UI reachable');
} catch (e) {
    console.error('E2E error:', e.message);
    failed++;
} finally {
    await browser.close();
}

if (failed > 0) {
    console.error(`\n${failed} check(s) failed`);
    process.exit(1);
}
console.log('\nAll E2E smoke checks passed');

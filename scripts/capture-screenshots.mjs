import { chromium } from 'playwright';
import { mkdir } from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const base = process.env.SCREENSHOT_BASE || 'http://localhost:8080';
const outDir = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'docs', 'screenshots');

await mkdir(outDir, { recursive: true });

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

async function shot(name, fn) {
    await page.goto(base, { waitUntil: 'networkidle', timeout: 120000 });
    if (fn) await fn();
    await page.waitForTimeout(800);
    await page.screenshot({ path: path.join(outDir, name), fullPage: false });
    console.log('OK', name);
}

await shot('home.png', async () => {
    await page.evaluate(() => window.scrollTo(0, 0));
});

await shot('product.png', async () => {
    const card = page.locator('.product-card').first();
    await card.waitFor({ timeout: 60000 });
    await card.click();
    await page.waitForSelector('#viewProduct.active, .product-page', { timeout: 30000 });
});

await shot('admin.png', async () => {
    await page.locator('#loginBtn').click();
    await page.waitForSelector('#loginModal[open], #loginForm', { timeout: 10000 });
    await page.fill('#loginForm input[name="email"]', 'admin@shop.com');
    await page.fill('#loginForm input[name="password"]', 'admin123');
    await page.locator('#loginForm button[type="submit"]').click();
    await page.waitForTimeout(2500);
    await page.locator('.nav-link[data-view="admin"]').click();
    await page.waitForSelector('#viewAdmin.active', { timeout: 20000 });
});

await page.setViewportSize({ width: 1280, height: 900 });
await page.goto(`${base}/swagger-ui.html`, { waitUntil: 'networkidle', timeout: 120000 });
await page.waitForTimeout(1000);
await page.screenshot({ path: path.join(outDir, 'swagger.png') });
console.log('OK', 'swagger.png');

await browser.close();

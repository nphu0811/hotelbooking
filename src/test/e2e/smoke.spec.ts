import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

function isoDate(daysFromNow: number): string {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + daysFromNow);
  return date.toISOString().slice(0, 10);
}

test('home, search, detail, auth and protected routes render', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('link', { name: /HotelBooking/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /Tìm phòng|Tim phong|Search/i })).toBeVisible();

  await page.goto(`/rooms/search?checkIn=${isoDate(1)}&checkOut=${isoDate(3)}&guests=2`);
  await expect(page.locator('.room-row, .empty').first()).toBeVisible();

  const detailLink = page.locator('.room-row a[href^="/rooms/"]').first();
  if (await detailLink.count()) {
    const href = await detailLink.getAttribute('href');
    await page.goto(href!);
    await expect(page.locator('.booking-box, .alert').first()).toBeVisible();
  }

  await page.goto('/login');
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();

  await page.goto('/register');
  await expect(page.locator('input[name="email"]')).toBeVisible();

  await page.goto('/admin');
  await expect(page).toHaveURL(/\/login/);
});

test('full authenticated booking, mock payment, refund, and admin review flow', async ({ page, request }, testInfo) => {
  const suffix = `${testInfo.project.name}-${Date.now()}`.replace(/[^a-zA-Z0-9-]/g, '-');
  const userEmail = `e2e-user-${suffix}@example.test`;
  const adminEmail = `e2e-admin-${suffix}@example.test`;
  const userName = `E2E Verified Customer ${suffix}`;
  const userPassword = `User-${suffix}@123`;
  const adminPassword = `Admin-${suffix}@123`;
  const hotelName = `E2E Overpass Verified Hotel ${suffix}`;
  const checkIn = isoDate(9);
  const checkOut = isoDate(11);

  expect((await request.post('/__e2e__/overpass-hotel', {
    form: { suffix }
  })).status()).toBe(200);
  expect((await request.post('/__e2e__/admin', {
    form: { email: adminEmail, password: adminPassword }
  })).status()).toBe(200);

  await page.goto('/register');
  await page.locator('input[name="fullName"]').fill(userName);
  await page.locator('input[name="email"]').fill(userEmail);
  await page.locator('input[name="phone"]').fill('0911111111');
  await page.locator('input[name="password"]').fill(userPassword);
  await page.locator('input[name="confirmPassword"]').fill(userPassword);
  await page.getByRole('button', { name: /Tạo tài khoản|Tao tai khoan|Register/i }).click();
  await expect(page).toHaveURL(/\/login\?registered/);

  expect((await request.post('/__e2e__/verify-user', {
    form: { email: userEmail }
  })).status()).toBe(200);

  await page.goto('/login');
  await page.locator('input[name="username"]').fill(userEmail);
  await page.locator('input[name="password"]').fill(userPassword);
  await page.getByRole('button', { name: /Đăng nhập|Dang nhap|Login/i }).click();
  await expect(page).toHaveURL('/');

  await page.goto(`/rooms/search?q=${encodeURIComponent(hotelName)}&checkIn=${checkIn}&checkOut=${checkOut}&guests=2`);
  await expect(page.getByText(hotelName)).toBeVisible();
  await expect(page.getByText('Source: OVERPASS')).toBeVisible();
  await page.getByRole('link', { name: /Chi tiết|Chi tiet|Detail/i }).click();
  await expect(page.getByText(hotelName)).toBeVisible();
  await expect(page.getByText('Source: OVERPASS')).toBeVisible();

  await page.locator('input[name="checkIn"]').fill(checkIn);
  await page.locator('input[name="checkOut"]').fill(checkOut);
  await page.locator('input[name="guests"]').fill('2');
  await page.locator('textarea[name="specialRequest"]').fill('E2E full booking flow');
  await page.getByRole('button', { name: /Đặt ngay|Dat ngay|Reserve/i }).click();
  await expect(page).toHaveURL(/\/checkout\//);
  await expect(page.getByText(hotelName)).toBeVisible();

  await page.getByRole('button', { name: /Continue to payment/i }).click();
  await expect(page).toHaveURL(/\/payments\/mock\//);
  await expect(page.getByText(/Order ID:/i)).toBeVisible();
  await page.getByRole('button', { name: /Mark paid/i }).click();
  await expect(page.getByText('PAID')).toBeVisible();
  await expect(page.getByText('CONFIRMED')).toBeVisible();
  await saveScreenshot(page, `e2e-payment-${testInfo.project.name}.png`);

  await page.getByRole('link', { name: /Xem lịch sử|Xem lich su|history/i }).click();
  await expect(page.getByText('CONFIRMED')).toBeVisible();
  await page.getByRole('link', { name: /Chi tiết|Chi tiet|Detail/i }).click();
  await expect(page.getByText('CONFIRMED')).toBeVisible();
  const cancelButton = page.getByRole('button', { name: /Hủy đặt phòng|Huy dat phong|Cancel/i });
  await expect(cancelButton).toBeVisible();
  await cancelButton.click({ force: true });
  await expect(page).toHaveURL(/\/account\/bookings\/.*cancelled/);
  await expect(page.getByText(/REFUNDED|CANCELLED/)).toBeVisible();

  await page.locator('form[action="/logout"] button').click();
  await expect(page).toHaveURL('/');
  await page.goto('/login');
  await page.locator('input[name="username"]').fill(adminEmail);
  await page.locator('input[name="password"]').fill(adminPassword);
  await page.getByRole('button', { name: /Đăng nhập|Dang nhap|Login/i }).click();
  await expect(page).toHaveURL('/');

  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: /Admin dashboard/i })).toBeVisible();
  await page.goto('/admin/bookings');
  await expect(page.getByText(userName)).toBeVisible();
  await expect(page.getByRole('row', { name: new RegExp(userName) }).getByText(/REFUNDED|CANCELLED|CONFIRMED/)).toBeVisible();
  await saveScreenshot(page, `e2e-admin-${testInfo.project.name}.png`);
});

async function saveScreenshot(page: import('@playwright/test').Page, name: string): Promise<void> {
  const dir = path.join('docs', 'qa-screenshots');
  await fs.mkdir(dir, { recursive: true });
  await page.screenshot({ path: path.join(dir, name), fullPage: true });
}

import { spawn } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";

const baseUrl = process.env.BASE_URL || "http://localhost:8081";
const chromePath = process.env.CHROME_PATH || "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const debugPort = Number(process.env.CHROME_DEBUG_PORT || 9223);
const screenshotDir = path.resolve("docs", "qa-screenshots");
const profileDir = path.join(tmpdir(), `hotelbooking-cdp-${Date.now()}`);
const checkIn = new Date(Date.now() + (760 + Math.floor(Math.random() * 90)) * 86400000)
  .toISOString()
  .slice(0, 10);
const checkOut = new Date(new Date(`${checkIn}T00:00:00.000Z`).getTime() + 2 * 86400000)
  .toISOString()
  .slice(0, 10);

await mkdir(screenshotDir, { recursive: true });

const chrome = spawn(chromePath, [
  `--remote-debugging-port=${debugPort}`,
  `--user-data-dir=${profileDir}`,
  "--no-first-run",
  "--no-default-browser-check",
  "--disable-default-apps",
  "--window-size=1440,900",
  "--new-window",
  "about:blank",
], {
  stdio: "ignore",
  windowsHide: false,
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`${url} returned ${response.status}`);
  }
  return response.json();
}

async function waitForDebugTarget() {
  const deadline = Date.now() + 15000;
  let lastError;
  while (Date.now() < deadline) {
    try {
      return await fetchJson(`http://127.0.0.1:${debugPort}/json/version`);
    } catch (error) {
      lastError = error;
      await sleep(200);
    }
  }
  throw lastError || new Error("Chrome DevTools target did not start");
}

class CdpClient {
  constructor(wsUrl) {
    this.ws = new WebSocket(wsUrl);
    this.nextId = 1;
    this.pending = new Map();
    this.events = [];
    this.consoleErrors = [];
    this.ws.onmessage = (message) => {
      const payload = JSON.parse(message.data);
      if (payload.id && this.pending.has(payload.id)) {
        const { resolve, reject } = this.pending.get(payload.id);
        this.pending.delete(payload.id);
        if (payload.error) {
          reject(new Error(payload.error.message || JSON.stringify(payload.error)));
        } else {
          resolve(payload.result || {});
        }
        return;
      }
      if (payload.method === "Runtime.exceptionThrown") {
        this.consoleErrors.push({
          text: payload.params.exceptionDetails.text || "Runtime exception",
          url: payload.params.exceptionDetails.url || "",
        });
      }
      if (payload.method === "Log.entryAdded" && payload.params.entry.level === "error") {
        this.consoleErrors.push({
          text: payload.params.entry.text,
          url: payload.params.entry.url || "",
        });
      }
      this.events.push(payload);
    };
  }

  async open() {
    if (this.ws.readyState === WebSocket.OPEN) {
      return;
    }
    await new Promise((resolve, reject) => {
      this.ws.onopen = resolve;
      this.ws.onerror = reject;
    });
  }

  send(method, params = {}) {
    const id = this.nextId++;
    const message = JSON.stringify({ id, method, params });
    const promise = new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
    this.ws.send(message);
    return promise;
  }
}

const version = await waitForDebugTarget();
let target;
try {
  target = await fetchJson(`http://127.0.0.1:${debugPort}/json/new?${encodeURIComponent("about:blank")}`, { method: "PUT" });
} catch {
  const targets = await fetchJson(`http://127.0.0.1:${debugPort}/json/list`);
  target = targets.find((entry) => entry.type === "page") || targets[0];
}

const cdp = new CdpClient(target.webSocketDebuggerUrl || version.webSocketDebuggerUrl);
await cdp.open();
await cdp.send("Page.enable");
await cdp.send("Runtime.enable");
await cdp.send("Log.enable");
await cdp.send("DOM.enable");

const results = [];
let viewport = { width: 1440, height: 900, scale: 1 };

function pass(name, detail = "") {
  results.push({ status: "PASS", name, detail });
}

function assert(condition, name, detail = "") {
  if (!condition) {
    throw new Error(`${name}${detail ? `: ${detail}` : ""}`);
  }
  pass(name, detail);
}

async function evaluate(expression) {
  const result = await cdp.send("Runtime.evaluate", {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text || "Runtime evaluation failed");
  }
  return result.result?.value;
}

async function waitReady() {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    const ready = await evaluate("document.readyState");
    if (ready === "interactive" || ready === "complete") {
      await sleep(250);
      return;
    }
    await sleep(100);
  }
  throw new Error("Page did not become ready");
}

async function navigate(url) {
  await cdp.send("Page.navigate", { url });
  await waitReady();
}

async function waitForUrl(fragment) {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    const href = await evaluate("location.href");
    if (href.includes(fragment)) {
      await waitReady();
      return href;
    }
    await sleep(150);
  }
  throw new Error(`URL did not include ${fragment}`);
}

async function setViewport(width, height, scale = 1) {
  viewport = { width, height, scale };
  await cdp.send("Emulation.setDeviceMetricsOverride", {
    width,
    height,
    deviceScaleFactor: scale,
    mobile: width < 700,
  });
  await cdp.send("Emulation.setVisibleSize", { width, height });
}

async function screenshot(name) {
  const file = path.join(screenshotDir, name);
  try {
    await cdp.send("Emulation.setVisibleSize", { width: viewport.width, height: viewport.height });
    const shot = await cdp.send("Page.captureScreenshot", {
      format: "png",
      fromSurface: true,
    });
    await writeFile(file, Buffer.from(shot.data, "base64"));
    return file;
  } catch (error) {
    results.push({ status: "WARN", name: `Screenshot ${name}`, detail: error.message });
    return "";
  }
}

async function clickByJs(selector, expectedUrlPart) {
  const clicked = await evaluate(`(() => {
    const el = document.querySelector(${JSON.stringify(selector)});
    if (!el) return false;
    el.click();
    return true;
  })()`);
  assert(clicked, `Click ${selector}`);
  if (expectedUrlPart) {
    await waitForUrl(expectedUrlPart);
  } else {
    await waitReady();
  }
}

async function submitForm(selector, expectedUrlPart) {
  const submitted = await evaluate(`(() => {
    const form = document.querySelector(${JSON.stringify(selector)});
    if (!form) return false;
    form.requestSubmit();
    return true;
  })()`);
  assert(submitted, `Submit ${selector}`);
  if (expectedUrlPart) {
    await waitForUrl(expectedUrlPart);
  } else {
    await waitReady();
  }
}

async function login(username, password) {
  await navigate(`${baseUrl}/login`);
  const filled = await evaluate(`(() => {
    document.querySelector('input[name="username"]').value = ${JSON.stringify(username)};
    document.querySelector('input[name="password"]').value = ${JSON.stringify(password)};
    document.querySelector('form.auth-panel').requestSubmit();
    return true;
  })()`);
  assert(filled, `Fill login for ${username}`);
  await waitReady();
  const loggedIn = await evaluate("!!document.querySelector(\"form[action='/logout'], form[action$='/logout']\") || !!document.querySelector(\"a[href='/account/bookings'], a[href$='/account/bookings']\")");
  assert(loggedIn, `Login ${username}`);
}

try {
  await setViewport(1440, 900);
  await navigate(`${baseUrl}/`);
  assert(await evaluate("!!document.querySelector('.topbar')"), "Home navbar visible");
  assert(await evaluate("!!document.querySelector('form.search-panel')"), "Home search form visible");
  assert(await evaluate("getComputedStyle(document.documentElement).getPropertyValue('--color-primary').trim().length > 0"), "CSS tokens loaded");
  await screenshot("01-home-desktop.png");

  const searched = await evaluate(`(() => {
    const form = document.querySelector('form.search-panel');
    form.querySelector('[name="checkIn"]').value = ${JSON.stringify(checkIn)};
    form.querySelector('[name="checkOut"]').value = ${JSON.stringify(checkOut)};
    form.querySelector('[name="guests"]').value = "2";
    form.requestSubmit();
    return true;
  })()`);
  assert(searched, "Submit home search");
  await waitForUrl("/rooms/search");
  assert(await evaluate("document.querySelectorAll('.room-row').length > 0"), "Search results render");
  await screenshot("02-search-results-desktop.png");

  await clickByJs(".room-row a[href*='/rooms/']", "/rooms/");
  assert(await evaluate("!!document.querySelector('.booking-box form')"), "Room detail booking form visible");
  assert(await evaluate("!!document.querySelector('input[name=\"roomId\"]') && !!document.querySelector('input[name=\"checkIn\"]') && !!document.querySelector('input[name=\"checkOut\"]') && !!document.querySelector('input[name=\"guests\"]')"), "Booking field names preserved");
  await screenshot("03-room-detail-desktop.png");

  const guestSubmitted = await evaluate(`(() => {
    document.querySelector('input[name="checkIn"]').value = ${JSON.stringify(checkIn)};
    document.querySelector('input[name="checkOut"]').value = ${JSON.stringify(checkOut)};
    document.querySelector('input[name="guests"]').value = "2";
    document.querySelector('.booking-box form').requestSubmit();
    return true;
  })()`);
  assert(guestSubmitted, "Submit booking as guest");
  await waitForUrl("/login");
  assert(await evaluate("!!document.querySelector('form.auth-panel')"), "Guest booking redirects to login");
  await screenshot("04-login-after-guest-booking.png");

  await login("customer@example.test", "User@123");
  await navigate(`${baseUrl}/rooms/search?checkIn=${checkIn}&checkOut=${checkOut}&guests=2`);
  await clickByJs(".room-row a[href*='/rooms/']", "/rooms/");
  const customerSubmitted = await evaluate(`(() => {
    document.querySelector('input[name="checkIn"]').value = ${JSON.stringify(checkIn)};
    document.querySelector('input[name="checkOut"]').value = ${JSON.stringify(checkOut)};
    document.querySelector('input[name="guests"]').value = "2";
    const note = document.querySelector('textarea[name="specialRequest"]');
    if (note) note.value = "Browser CDP QA";
    document.querySelector('.booking-box form').requestSubmit();
    return true;
  })()`);
  assert(customerSubmitted, "Submit booking as customer");
  const checkoutUrl = await waitForUrl("/checkout/");
  const bookingId = checkoutUrl.match(/checkout\/([0-9a-fA-F-]{36})/)?.[1] || "";
  assert(bookingId.length === 36, "Checkout URL contains booking id", bookingId);
  await screenshot("05-checkout.png");

  await submitForm("form[action*='/payments/mock/start/']", "/payments/mock/");
  assert(await evaluate("document.body.innerText.includes('Order ID')"), "Mock payment page visible");
  await screenshot("06-mock-payment.png");

  await clickByJs("a[href*='payments/mock/callback'][href*='success=true']", "/payments/mock/callback");
  assert(await evaluate("document.body.innerText.includes('SUCCESS') && document.body.innerText.includes('CONFIRMED')"), "Payment success confirms booking");
  await screenshot("07-payment-result.png");

  await clickByJs("a[href='/account/bookings'], a[href*='/account/bookings']", "/account/bookings");
  assert(await evaluate(`document.body.innerText.includes(${JSON.stringify(bookingId)}) || document.querySelectorAll('table tbody tr').length > 0`), "Booking history renders");
  await screenshot("08-booking-history.png");

  await submitForm("form[action='/logout'], form[action$='/logout']");
  await waitReady();
  await login("admin@example.test", "Admin@123");
  await navigate(`${baseUrl}/admin`);
  assert(await evaluate("document.body.innerText.includes('Admin') || document.querySelectorAll('.stat-card').length > 0"), "Admin dashboard renders");
  await screenshot("09-admin-dashboard.png");

  await navigate(`${baseUrl}/admin/rooms`);
  assert(await evaluate("document.querySelectorAll('table tbody tr').length > 0"), "Admin rooms table renders");
  await navigate(`${baseUrl}/admin/bookings`);
  assert(await evaluate("document.querySelectorAll('table tbody tr').length > 0"), "Admin bookings table renders");
  await navigate(`${baseUrl}/admin/users`);
  assert(await evaluate("document.querySelectorAll('table tbody tr').length > 0"), "Admin users table renders");

  await setViewport(430, 900, 2);
  await navigate(`${baseUrl}/`);
  assert(await evaluate("document.documentElement.scrollWidth <= window.innerWidth + 2"), "Mobile home has no horizontal overflow");
  await screenshot("10-home-mobile.png");
  await navigate(`${baseUrl}/rooms/search?checkIn=${checkIn}&checkOut=${checkOut}&guests=2`);
  assert(await evaluate("document.documentElement.scrollWidth <= window.innerWidth + 2"), "Mobile search has no horizontal overflow");
  await screenshot("11-search-mobile.png");
  await navigate(`${baseUrl}/login?error&captcha`);
  assert(await evaluate("!!document.querySelector('.captcha-hook')"), "CAPTCHA hook visible");
  assert(await evaluate("document.documentElement.scrollWidth <= window.innerWidth + 2"), "Mobile login has no horizontal overflow");
  await screenshot("12-login-captcha-mobile.png");

  const relevantConsoleErrors = cdp.consoleErrors.filter((entry) => {
    const text = entry.text || "";
    const url = entry.url || "";
    return !(text.includes("404") && (url.endsWith("/favicon.ico") || url === ""));
  });
  assert(relevantConsoleErrors.length === 0, "No browser console/runtime errors", JSON.stringify(relevantConsoleErrors));

  await cdp.send("Browser.close").catch(() => {});
  chrome.kill();

  console.log(JSON.stringify({
    baseUrl,
    checkIn,
    checkOut,
    screenshots: screenshotDir,
    results,
  }, null, 2));
} catch (error) {
  await screenshot("failure.png").catch(() => {});
  console.error(JSON.stringify({
    baseUrl,
    checkIn,
    checkOut,
    screenshots: screenshotDir,
    results,
    error: error.message,
    consoleErrors: cdp.consoleErrors,
  }, null, 2));
  chrome.kill();
  process.exit(1);
}

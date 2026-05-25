import { defineConfig, devices } from '@playwright/test';

const appCommand = 'java -jar build/libs/HotelBooking-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --app.e2e-fixture.enabled=true';

export default defineConfig({
  testDir: './src/test/e2e',
  timeout: 60_000,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:8080',
    trace: 'retain-on-failure'
  },
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: appCommand,
        url: 'http://127.0.0.1:8080/actuator/health',
        reuseExistingServer: true,
        timeout: 120_000
      },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] }
    }
  ]
});

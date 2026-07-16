const { test, expect } = require('@playwright/test');

test.describe('Currency Transaction Service - E2E API Tests', () => {

  test('POST /api/v1/transactions - should create a new transaction with valid payload', async ({ request }) => {
    const response = await request.post('/api/v1/transactions', {
      data: {
        description: 'Mechanical Keyboard purchase',
        transactionDate: '2023-12-15',
        purchaseAmount: 129.99
      }
    });

    expect(response.status()).toBe(201);
    const body = await response.json();
    expect(body.id).toBeDefined();
    expect(body.description).toBe('Mechanical Keyboard purchase');
    expect(body.transactionDate).toBe('2023-12-15');
    expect(body.purchaseAmount).toBe(129.99);
  });

  test('GET /api/v1/transactions/{id} - should convert transaction successfully with valid targetCurrency and exchange rate within 6 months', async ({ request }) => {
    // 1. Create a transaction
    const postResponse = await request.post('/api/v1/transactions', {
      data: {
        description: 'Canada-Dollar Conversion Test',
        transactionDate: '2023-12-15',
        purchaseAmount: 100.00
      }
    });
    expect(postResponse.status()).toBe(201);
    const transaction = await postResponse.json();
    const uuid = transaction.id;

    // 2. Fetch converted transaction
    const getResponse = await request.get(`/api/v1/transactions/${uuid}`, {
      params: {
        targetCurrency: 'Canada-Dollar'
      }
    });

    expect(getResponse.status()).toBe(200);
    const result = await getResponse.json();
    expect(result.id).toBe(uuid);
    expect(result.description).toBe('Canada-Dollar Conversion Test');
    expect(result.transactionDate).toBe('2023-12-15');
    expect(result.originalAmountUsd).toBe(100.00);
    expect(result.targetCurrency).toBe('Canada-Dollar');
    expect(result.exchangeRate).toBeDefined();
    expect(result.convertedAmount).toBeDefined();
    
    // Verify mathematical conversion precision (convertedAmount = originalAmountUsd * exchangeRate, rounded to 2 decimals)
    const expectedAmount = Number((100.00 * result.exchangeRate).toFixed(2));
    expect(result.convertedAmount).toBe(expectedAmount);
  });

  test('GET /api/v1/transactions/{id} - should return 404 when transaction UUID does not exist', async ({ request }) => {
    const nonExistentUuid = '00000000-0000-0000-0000-000000000000';
    const response = await request.get(`/api/v1/transactions/${nonExistentUuid}`, {
      params: {
        targetCurrency: 'Canada-Dollar'
      }
    });

    expect(response.status()).toBe(404);
    const body = await response.json();
    expect(body.error).toBe(`Transaction not found with ID: ${nonExistentUuid}`);
  });

  test('GET /api/v1/transactions/{id} - should return 400 with 6-month threshold validation error when purchase date is older than 6 months from closest rate', async ({ request }) => {
    // 1. Create a transaction from the year 2000 (which will not have any rates within 6 months)
    const postResponse = await request.post('/api/v1/transactions', {
      data: {
        description: 'Old Transaction',
        transactionDate: '2000-01-01',
        purchaseAmount: 50.00
      }
    });
    expect(postResponse.status()).toBe(201);
    const transaction = await postResponse.json();
    const uuid = transaction.id;

    // 2. Try to fetch converted transaction
    const getResponse = await request.get(`/api/v1/transactions/${uuid}`, {
      params: {
        targetCurrency: 'Canada-Dollar'
      }
    });

    expect(getResponse.status()).toBe(400);
    const body = await getResponse.json();
    expect(body.error).toBe('No currency conversion rate is available within 6 months equal to or before the purchase date.');
  });

  test('POST /api/v1/transactions - validation error: description exceeds 50 characters', async ({ request }) => {
    const longDescription = 'A'.repeat(51);
    const response = await request.post('/api/v1/transactions', {
      data: {
        description: longDescription,
        transactionDate: '2023-12-15',
        purchaseAmount: 10.00
      }
    });

    expect(response.status()).toBe(400);
  });

  test('POST /api/v1/transactions - validation error: purchaseAmount less than 0.01', async ({ request }) => {
    const response = await request.post('/api/v1/transactions', {
      data: {
        description: 'Invalid Amount Test',
        transactionDate: '2023-12-15',
        purchaseAmount: 0.00
      }
    });

    expect(response.status()).toBe(400);
  });

  test('POST /api/v1/transactions - validation error: missing description', async ({ request }) => {
    const response = await request.post('/api/v1/transactions', {
      data: {
        transactionDate: '2023-12-15',
        purchaseAmount: 10.00
      }
    });

    expect(response.status()).toBe(400);
  });
});

test.describe('Currency Transaction Service - Swagger UI E2E Interactive Tests', () => {

  test('should allow transaction creation and conversion inside Swagger UI web playground', async ({ page }) => {
    // 1. Navigate to Swagger UI
    await page.goto('/swagger-ui/index.html');
    await expect(page).toHaveTitle(/Swagger UI/i);
    await page.screenshot({ path: './results/swagger_ui_landing.png' });

    // 2. Find and expand POST endpoint
    const postHeader = page.locator('.opblock-post');
    await postHeader.click();
    await expect(postHeader.locator('.opblock-body')).toBeVisible();

    // 3. Click Try it out
    const postTryOutBtn = postHeader.locator('button.try-out__btn');
    await postTryOutBtn.click();

    // 4. Fill Request Body
    const requestBodyArea = postHeader.locator('textarea.body-param__text');
    await requestBodyArea.clear();
    const testJson = {
      description: 'Swagger UI E2E Purchase',
      transactionDate: '2023-12-15',
      purchaseAmount: 89.95
    };
    await requestBodyArea.fill(JSON.stringify(testJson, null, 2));

    // 5. Execute POST
    const postExecuteBtn = postHeader.locator('button.execute');
    await postExecuteBtn.click();

    // 6. Verify POST status and get UUID
    const postResponseStatusCode = postHeader.locator('.live-responses-table .response-col_status:not(.col_header)');
    await expect(postResponseStatusCode).toContainText('201');
    await page.screenshot({ path: './results/swagger_ui_post_success.png' });

    const postResponseBodyText = await postHeader.locator('.live-responses-table pre.microlight').first().innerText();
    const transaction = JSON.parse(postResponseBodyText);
    expect(transaction.id).toBeDefined();
    const uuid = transaction.id;

    // 7. Find and expand GET endpoint
    const getHeader = page.locator('.opblock-get');
    await getHeader.click();
    await expect(getHeader.locator('.opblock-body')).toBeVisible();

    // 8. Click Try it out
    const getTryOutBtn = getHeader.locator('button.try-out__btn');
    await getTryOutBtn.click();

    // 9. Fill path and query parameters
    const getUuidInput = getHeader.locator('tr[data-param-name="id"] input');
    await getUuidInput.fill(uuid);

    const getCurrencyInput = getHeader.locator('tr[data-param-name="targetCurrency"] input');
    await getCurrencyInput.fill('Canada-Dollar');

    // 10. Execute GET
    const getExecuteBtn = getHeader.locator('button.execute');
    await getExecuteBtn.click();

    // 11. Verify GET status and conversion
    const getResponseStatusCode = getHeader.locator('.live-responses-table .response-col_status:not(.col_header)');
    await expect(getResponseStatusCode).toContainText('200');
    await page.screenshot({ path: './results/swagger_ui_get_success.png' });

    const getResponseBodyText = await getHeader.locator('.live-responses-table pre.microlight').first().innerText();
    const conversionResult = JSON.parse(getResponseBodyText);
    expect(conversionResult.id).toBe(uuid);
    expect(conversionResult.targetCurrency).toBe('Canada-Dollar');
    expect(conversionResult.originalAmountUsd).toBe(89.95);
    expect(conversionResult.convertedAmount).toBeDefined();
  });
});

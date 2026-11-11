require('./helpers');
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { lookupBarcode, toGtin14, mapUsdaError } = require('../src/services/productService');
const { config } = require('../src/config/env');
const { validateBarcodeParam } = require('../src/middleware/validate');
const { barcode: barcodeController } = require('../src/controllers/productController');

/** Replaces the global fetch with a fake USDA. Returns the URLs it was asked for. */
function fakeUsda(handler) {
  const urls = [];
  globalThis.fetch = async (url) => {
    urls.push(String(url));
    return handler();
  };
  return urls;
}

const ok = (foods) => ({ ok: true, status: 200, json: async () => ({ foods }) });
const fail = (status) => ({ ok: false, status, json: async () => ({}) });

beforeEach(() => {
  config.usda.apiKey = 'test-usda-key';
});

test('toGtin14: pads UPC-A and EAN-13 with zeros', () => {
  assert.equal(toGtin14('049000028911'), '00049000028911');
  assert.equal(toGtin14('4006381333931'), '04006381333931');
  assert.equal(toGtin14('00049000028911'), '00049000028911');
});

test('lookupBarcode: returns the description of the product whose gtinUpc matches', async () => {
  const urls = fakeUsda(() => ok([
    { gtinUpc: '049000050103', description: 'OTHER PRODUCT' },
    { gtinUpc: '049000028911', description: ' COCA-COLA ' },
  ]));

  const product = await lookupBarcode('049000028911');

  assert.deepEqual(product, { barcode: '049000028911', name: 'COCA-COLA' });
  assert.ok(urls[0].includes('query=00049000028911')); // 12 digits are sent as a GTIN-14
});

test('lookupBarcode: a fuzzy USDA result with another barcode is not accepted', async () => {
  fakeUsda(() => ok([{ gtinUpc: '049000050103', description: 'OTHER PRODUCT' }]));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_NOT_FOUND', statusCode: 404 });
});

test('lookupBarcode: no foods, a missing gtin or a blank description is PRODUCT_NOT_FOUND', async () => {
  fakeUsda(() => ok([]));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_NOT_FOUND' });
  fakeUsda(() => ok([{ description: 'NO GTIN' }]));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_NOT_FOUND' });
  fakeUsda(() => ok([{ gtinUpc: '049000028911', description: '  ' }]));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_NOT_FOUND' });
});

test('lookupBarcode: without a USDA key it fails with a clear 503 and never calls USDA', async () => {
  config.usda.apiKey = '';
  const urls = fakeUsda(() => ok([]));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_LOOKUP_NOT_CONFIGURED', statusCode: 503 });
  assert.equal(urls.length, 0);
});

test('lookupBarcode: network failures and USDA errors become safe codes', async () => {
  globalThis.fetch = async () => { throw Object.assign(new Error('x'), { name: 'TimeoutError' }); };
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_TIMEOUT' });
  fakeUsda(() => fail(429));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_RATE_LIMITED' });
  fakeUsda(() => fail(403));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_CONFIG_ERROR' });
  fakeUsda(() => fail(500));
  await assert.rejects(lookupBarcode('049000028911'), { code: 'PRODUCT_UNAVAILABLE' });
});

test('mapUsdaError: the message never contains the API key', () => {
  const err = mapUsdaError(null, 403);
  assert.ok(!JSON.stringify(err.message).includes('test-usda-key'));
});

/** Runs validateBarcodeParam like Express would and returns what it passed to next(). */
function validate(code) {
  const req = { params: { code } };
  let passed;
  validateBarcodeParam(req, {}, (err) => { passed = err; });
  return { req, err: passed };
}

test('validateBarcodeParam: accepts 8, 12, 13 and 14 digits and trims spaces', () => {
  for (const code of ['12345678', '049000028911', '4006381333931', '00049000028911', ' 049000028911 ']) {
    const { req, err } = validate(code);
    assert.equal(err, undefined, code);
    assert.equal(req.validatedBarcode, code.trim());
  }
});

test('validateBarcodeParam: rejects other lengths and non-digits with a 400', () => {
  for (const code of ['123', '1234567', '123456789', 'abcdefgh', '04900002891x', '']) {
    const { err } = validate(code);
    assert.equal(err.statusCode, 400, code);
    assert.equal(err.code, 'VALIDATION_ERROR');
  }
});

test('barcode controller: answers 200 with the product', async () => {
  fakeUsda(() => ok([{ gtinUpc: '049000028911', description: 'COCA-COLA' }]));
  let status;
  let body;
  const res = { status(code) { status = code; return this; }, json(b) { body = b; } };

  await barcodeController({ validatedBarcode: '049000028911' }, res);

  assert.equal(status, 200);
  assert.deepEqual(body, { barcode: '049000028911', name: 'COCA-COLA' });
});

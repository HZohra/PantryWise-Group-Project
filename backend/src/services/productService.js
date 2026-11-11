const { config } = require('../config/env');
const AppError = require('../utils/AppError');

const BASE_URL = 'https://api.nal.usda.gov/fdc/v1/foods/search';

/** Pads a barcode with zeros to the 14-digit GTIN form USDA stores (UPC-A has 12 digits, EAN-13 has 13). */
function toGtin14(barcode) {
  return String(barcode).padStart(14, '0');
}

/** Builds the request URL. Contains the API key: NEVER log the result. */
function buildUrl(barcode) {
  const params = new URLSearchParams({
    query: toGtin14(barcode),
    dataType: 'Branded',
    api_key: config.usda.apiKey,
  });
  return `${BASE_URL}?${params.toString()}`;
}

/** Converts fetch/HTTP failures into safe AppErrors. Logs status only, never the URL. */
function mapUsdaError(err, status) {
  if (err && (err.name === 'TimeoutError' || err.name === 'AbortError')) {
    console.error('[usda] request timed out');
    return new AppError(504, 'PRODUCT_TIMEOUT', 'The product lookup took too long. Please try again.');
  }
  if (err) {
    console.error('[usda] network error:', err.name);
    return new AppError(503, 'PRODUCT_UNAVAILABLE', 'The product lookup is unavailable right now. Please try again later.');
  }
  console.error('[usda] request failed with status', status);
  if (status === 429) {
    return new AppError(429, 'PRODUCT_RATE_LIMITED', 'Too many product lookups. Please wait a moment and try again.');
  }
  if (status === 401 || status === 403) {
    return new AppError(502, 'PRODUCT_CONFIG_ERROR', 'The product lookup is not configured correctly.');
  }
  return new AppError(503, 'PRODUCT_UNAVAILABLE', 'The product lookup is unavailable right now. Please try again later.');
}

/**
 * Finds the product name of a barcode in USDA FoodData Central (branded foods).
 * USDA's search is fuzzy, so the result is only accepted when its gtinUpc really equals the barcode.
 * @param {string} barcode  digits only (8, 12, 13 or 14), already validated
 * @returns {Promise<{barcode: string, name: string}>}
 * @throws {AppError} 503 PRODUCT_LOOKUP_NOT_CONFIGURED without a key, 404 PRODUCT_NOT_FOUND, or a mapped USDA error
 */
async function lookupBarcode(barcode) {
  if (!config.usda.apiKey || !config.usda.apiKey.trim()) {
    throw new AppError(503, 'PRODUCT_LOOKUP_NOT_CONFIGURED', 'The product lookup is not configured on the server.');
  }

  console.log('[usda] request'); // no barcode or URL in logs
  let response;
  try {
    response = await fetch(buildUrl(barcode), { signal: AbortSignal.timeout(config.usda.timeoutMs) });
  } catch (err) {
    throw mapUsdaError(err);
  }
  if (!response.ok) {
    throw mapUsdaError(null, response.status);
  }

  const data = await response.json();
  const wanted = toGtin14(barcode);
  const match = (data.foods || []).find((food) =>
    typeof food.gtinUpc === 'string' && /^\d+$/.test(food.gtinUpc) && toGtin14(food.gtinUpc) === wanted);

  const name = match && typeof match.description === 'string' ? match.description.trim() : '';
  if (!name) {
    throw new AppError(404, 'PRODUCT_NOT_FOUND', 'No product with this barcode was found.');
  }
  return { barcode: String(barcode), name };
}

module.exports = { lookupBarcode, toGtin14, mapUsdaError };

const { lookupBarcode } = require('../services/productService');

/** GET /api/v1/products/barcode/:code → 200 { barcode, name } */
async function barcode(req, res) {
  const product = await lookupBarcode(req.validatedBarcode);
  res.status(200).json(product);
}

module.exports = { barcode };

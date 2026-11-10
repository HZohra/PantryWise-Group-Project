
package com.cp470.groupproject.scanner;

import android.util.Log;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.BuildConfig;
import com.cp470.groupproject.scanner.api.RetrofitClient;
import com.cp470.groupproject.scanner.api.UsdaApiService;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarcodeViewModel extends ViewModel {

    private final MutableLiveData<String> productName = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final UsdaApiService usdaApiService =
            RetrofitClient.getUsdaApiService();

    public LiveData<String> getProductName() {
        return productName;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void lookupProduct(String barcode) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        productName.setValue(null);

        // Validate the barcode
        if (barcode == null || !barcode.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            errorMessage.setValue(
                    "Invalid barcode. Please scan a valid UPC or EAN barcode."
            );
            isLoading.setValue(false);
            return;
        }

        // Convert 12-digit UPC-A to 14-digit GTIN for USDA lookup
        String searchBarcode = barcode;

        if (barcode.length() == 12) {
            searchBarcode = "00" + barcode;
        }

        // Request product information from USDA
        Call<JsonObject> call = usdaApiService.searchFood(
                searchBarcode,
                "Branded",
                BuildConfig.USDA_API_KEY
        );

        call.enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading.setValue(false);

                // Log the HTTP response code to help identify API errors
                Log.d("BarcodeViewModel",
                        "USDA response code: " + response.code());

                // Check whether the API request was successful
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e("BarcodeViewModel",
                            "USDA request unsuccessful. HTTP code: " + response.code());

                    errorMessage.setValue(
                            "Unable to retrieve product information. Please try again."
                    );
                    return;
                }

                // Get the list of products returned by USDA
                JsonObject jsonResponse = response.body();
                JsonArray foods = jsonResponse.getAsJsonArray("foods");

                Log.d("BarcodeViewModel",
                        "USDA returned " + (foods == null ? 0 : foods.size()) + " products");

                // Check if any products were found
                if (foods == null || foods.size() == 0) {
                    errorMessage.setValue(
                            "Product not found. Please enter the food manually."
                    );
                    return;
                }

                // Find a product with the same barcode
                JsonObject matchedProduct = null;

                for (int i = 0; i < foods.size(); i++) {
                    JsonObject food = foods.get(i).getAsJsonObject();

                    if (food.has("gtinUpc") && !food.get("gtinUpc").isJsonNull()) {
                        String productBarcode = food.get("gtinUpc").getAsString();

                        // Compare barcodes in their 14-digit GTIN format
                        String normalizedScannedBarcode =
                                String.format("%014d", Long.parseLong(barcode));

                        String normalizedProductBarcode =
                                String.format("%014d", Long.parseLong(productBarcode));

                        if (normalizedProductBarcode.equals(normalizedScannedBarcode)) {
                            matchedProduct = food;
                            break;
                        }
                    }
                }

                // No product matched the scanned barcode
                if (matchedProduct == null) {
                    errorMessage.setValue(
                            "Product not found in USDA database. Please enter it manually."
                    );
                    return;
                }

                // Get the product name from the matching result
                if (matchedProduct.has("description")
                        && !matchedProduct.get("description").isJsonNull()) {

                    String name = matchedProduct.get("description").getAsString();

                    productName.setValue(name);

                } else {
                    errorMessage.setValue(
                            "Product information is incomplete. Please enter it manually."
                    );
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading.setValue(false);

                Log.e("BarcodeViewModel",
                        "USDA API request failed", t);

                errorMessage.setValue("Network error. Please try again.");
            }
        });
    }
}

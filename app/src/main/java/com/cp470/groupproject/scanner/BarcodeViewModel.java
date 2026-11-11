
package com.cp470.groupproject.scanner;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiApiService;
import com.cp470.groupproject.data.remote.ApiClient;
import com.cp470.groupproject.data.remote.dto.ProductResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * State and logic of the barcode scanner: validates a barcode and asks the PantryWise backend for
 * the product name. The backend holds the USDA key, so the app never contains it.
 */
public class BarcodeViewModel extends ViewModel {

    private static final String TAG = "BarcodeViewModel";

    /** HTTP status the backend uses when the barcode is not in the USDA database. */
    private static final int HTTP_NOT_FOUND = 404;

    private final MutableLiveData<String> productName = new MutableLiveData<>();
    private final MutableLiveData<Integer> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final AiApiService apiService;

    /** Creates the view model with the shared backend client. */
    public BarcodeViewModel() {
        this(ApiClient.getService());
    }

    /** Lets tests pass a fake backend. */
    public BarcodeViewModel(@NonNull AiApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<String> getProductName() {
        return productName;
    }

    /** string resource id of the last lookup error, so this class needs no context */
    @StringRes
    public LiveData<Integer> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Checks the barcode and looks up the product name. The result arrives in {@link #getProductName()}
     * or, when something goes wrong, as a string resource id in {@link #getErrorMessage()}.
     *
     * @param barcode digits of a UPC or EAN code (8, 12, 13 or 14 digits)
     */
    public void lookupProduct(String barcode) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        productName.setValue(null);

        if (barcode == null || !barcode.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            errorMessage.setValue(R.string.scanner_error_invalid_barcode_scan);
            isLoading.setValue(false);
            return;
        }

        apiService.lookupProduct(barcode).enqueue(new Callback<ProductResponse>() {

            @Override
            public void onResponse(@NonNull Call<ProductResponse> call,
                                   @NonNull Response<ProductResponse> response) {
                isLoading.setValue(false);
                Log.d(TAG, "lookup response code: " + response.code());

                if (response.code() == HTTP_NOT_FOUND) {
                    errorMessage.setValue(R.string.scanner_error_not_in_database);
                    return;
                }
                ProductResponse body = response.body();
                if (!response.isSuccessful() || body == null
                        || body.name == null || body.name.trim().isEmpty()) {
                    errorMessage.setValue(R.string.scanner_error_lookup_failed);
                    return;
                }
                productName.setValue(body.name.trim());
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                Log.e(TAG, "barcode lookup failed", t);
                errorMessage.setValue(R.string.scanner_error_network);
            }
        });
    }
}

package com.cp470.groupproject.scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiApiService;
import com.cp470.groupproject.data.remote.dto.ProductResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tests of the barcode lookup. the backend is a mockito mock, so no network is used: each test
 * answers the captured callback with the response it wants to check.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class BarcodeViewModelTest {

    @Rule
    public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    private AiApiService api;
    private Call<ProductResponse> call;
    private BarcodeViewModel viewModel;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        api = Mockito.mock(AiApiService.class);
        call = Mockito.mock(Call.class);
        when(api.lookupProduct(anyString())).thenReturn(call);
        viewModel = new BarcodeViewModel(api);
    }

    @SuppressWarnings("unchecked")
    private Callback<ProductResponse> captureCallback() {
        ArgumentCaptor<Callback<ProductResponse>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(call).enqueue(captor.capture());
        return captor.getValue();
    }

    private static ProductResponse product(String name) {
        ProductResponse response = new ProductResponse();
        response.barcode = "049000028911";
        response.name = name;
        return response;
    }

    @Test
    public void lookupProduct_invalidBarcode_setsErrorAndDoesNotCallBackend() {
        viewModel.lookupProduct("123");

        assertEquals(Integer.valueOf(R.string.scanner_error_invalid_barcode_scan),
                viewModel.getErrorMessage().getValue());
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
        verify(api, never()).lookupProduct(anyString());
    }

    @Test
    public void lookupProduct_nullBarcode_setsError() {
        viewModel.lookupProduct(null);

        assertEquals(Integer.valueOf(R.string.scanner_error_invalid_barcode_scan),
                viewModel.getErrorMessage().getValue());
    }

    @Test
    public void lookupProduct_validBarcode_isLoadingUntilTheAnswerArrives() {
        viewModel.lookupProduct("049000028911");

        assertEquals(Boolean.TRUE, viewModel.getIsLoading().getValue());
        verify(api).lookupProduct("049000028911");
    }

    @Test
    public void lookupProduct_success_publishesTheTrimmedName() {
        viewModel.lookupProduct("049000028911");

        captureCallback().onResponse(call, Response.success(product("  COCA-COLA ")));

        assertEquals("COCA-COLA", viewModel.getProductName().getValue());
        assertNull(viewModel.getErrorMessage().getValue());
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void lookupProduct_http404_saysTheProductIsNotInTheDatabase() {
        viewModel.lookupProduct("049000028911");

        captureCallback().onResponse(call, Response.error(404,
                ResponseBody.create(MediaType.parse("application/json"), "{}")));

        assertEquals(Integer.valueOf(R.string.scanner_error_not_in_database),
                viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getProductName().getValue());
    }

    @Test
    public void lookupProduct_http503_saysTheLookupFailed() {
        viewModel.lookupProduct("049000028911");

        captureCallback().onResponse(call, Response.error(503,
                ResponseBody.create(MediaType.parse("application/json"), "{}")));

        assertEquals(Integer.valueOf(R.string.scanner_error_lookup_failed),
                viewModel.getErrorMessage().getValue());
    }

    @Test
    public void lookupProduct_blankName_saysTheLookupFailed() {
        viewModel.lookupProduct("049000028911");

        captureCallback().onResponse(call, Response.success(product("   ")));

        assertEquals(Integer.valueOf(R.string.scanner_error_lookup_failed),
                viewModel.getErrorMessage().getValue());
    }

    @Test
    public void lookupProduct_networkFailure_saysNetworkError() {
        viewModel.lookupProduct("049000028911");

        captureCallback().onFailure(call, new IOException("no route"));

        assertEquals(Integer.valueOf(R.string.scanner_error_network),
                viewModel.getErrorMessage().getValue());
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void lookupProduct_afterAnError_aNewLookupClearsIt() {
        viewModel.lookupProduct("123");
        viewModel.lookupProduct("049000028911");

        assertNull(viewModel.getErrorMessage().getValue());
    }
}

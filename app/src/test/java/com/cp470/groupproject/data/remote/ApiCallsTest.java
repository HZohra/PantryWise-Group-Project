package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ApiCallsTest {

    private static Response<Object> errorResponse(int code, String body, String retryAfter) {
        okhttp3.Response.Builder raw = new okhttp3.Response.Builder()
                .code(code)
                .message("error")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build());
        if (retryAfter != null) raw.header("Retry-After", retryAfter);
        return Response.error(ResponseBody.create(body, MediaType.get("application/json")), raw.build());
    }

    @Test
    public void parseError_readsCodeStatusAndRetryAfter() {
        String json = "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}}";

        AiError error = ApiCalls.parseError(errorResponse(429, json, "30"));

        assertEquals(AiError.RATE_LIMITED, error.code);
        assertEquals(429, error.httpStatus);
        assertEquals(Integer.valueOf(30), error.retryAfterSeconds);
        assertTrue(error.isRateLimited());
    }

    @Test
    public void parseError_emptyBody_isUnexpectedResponse() {
        AiError error = ApiCalls.parseError(errorResponse(502, "", null));

        assertEquals(AiError.UNEXPECTED_RESPONSE, error.code);
        assertEquals(502, error.httpStatus);
        assertNull(error.retryAfterSeconds);
    }
}
package com.braintreepayments.api.internal;

import com.braintreepayments.api.Json;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.GraphQLConstants.ErrorTypes;
import com.braintreepayments.api.internal.GraphQLConstants.Headers;
import com.braintreepayments.api.internal.GraphQLConstants.Keys;
import com.braintreepayments.api.internal.GraphQLConstants.LegacyErrorCodes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

public class BraintreeGraphQLHttpClient extends BraintreeApiHttpClient {
    public BraintreeGraphQLHttpClient(String baseUrl, String bearer) {
        this(baseUrl, bearer, Headers.API_VERSION);
    }

    private BraintreeGraphQLHttpClient(String baseUrl, String bearer, String apiVersion) {
        super(baseUrl, bearer, apiVersion);

        try {
            setSSLSocketFactory(new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    public void post(String data, HttpResponseCallback callback) {
        super.post("", data, callback);
    }

    @Override
    protected String parseResponse(HttpURLConnection connection) throws Exception {
        String response = super.parseResponse(connection);
        JSONArray errors = new JSONObject(response)
                .optJSONArray(Keys.ERRORS);

        if (errors != null) {
            for (int i = 0; i < errors.length(); i++) {
                JSONObject error = errors.getJSONObject(i);
                JSONObject extensions = error.optJSONObject(Keys.EXTENSIONS);

                if (extensions == null) {
                    throw new UnexpectedException("An unexpected error occurred");
                }

                if (Json.optString(extensions, Keys.LEGACY_CODE, "").equals(LegacyErrorCodes.VALIDATION_NOT_ALLOWED)) {
                    throw new AuthorizationException(error.getString(Keys.MESSAGE));
                } else if (!Json.optString(extensions, Keys.ERROR_TYPE, "").equals(ErrorTypes.USER)) {
                    throw new UnexpectedException("An unexpected error occurred");
                }
            }

            throw ErrorWithResponse.fromGraphQLJson(response);
        }

        return response;
    }
}

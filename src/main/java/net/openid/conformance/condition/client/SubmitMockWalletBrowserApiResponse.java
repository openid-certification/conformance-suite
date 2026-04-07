package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Integration-test-only condition: wraps the prepared VP response in DC API format
 * and POSTs it to the wallet test's own browser API submit URL, simulating a real
 * wallet's DC API response.
 *
 * Expects the caller to have already generated the credential, built the VP token,
 * and optionally encrypted the response using the standard conditions
 * (CreateSdJwtKbCredential, AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams,
 * VP1FinalEncryptVPResponse).
 *
 * Uses callEndpointWithJsonBody (which calls createRestTemplate) so the lock is
 * released during the HTTP call, allowing handleBrowserApiSubmission to process
 * the response without deadlocking.
 */
public class SubmitMockWalletBrowserApiResponse extends AbstractCallEndpointWithPost {

	private static final String RESPONSE_ENV_KEY = "mock_wallet_submit_response";

	@Override
	@PreEnvironment(required = {"browser_api_request", "browser_api_submit"})
	public Environment evaluate(Environment env) {

		String submitUrl = env.getString("browser_api_submit", "fullUrl");

		JsonArray requests = env.getElementFromObject("browser_api_request", "digital.requests").getAsJsonArray();
		String protocol = OIDFJSON.getString(requests.get(0).getAsJsonObject().get("protocol"));

		JsonObject dcApiResponse = new JsonObject();
		dcApiResponse.addProperty("protocol", protocol);

		// If VP1FinalEncryptVPResponse ran, the encrypted response is in direct_post_request_form_parameters.
		// Otherwise use the unencrypted authorization_endpoint_response_params.
		JsonObject formParams = env.getObject("direct_post_request_form_parameters");
		boolean encrypted = formParams != null && formParams.has("response");
		if (encrypted) {
			dcApiResponse.add("data", formParams);
		} else {
			dcApiResponse.add("data", env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY));
		}

		callEndpointWithJsonBody(env, dcApiResponse.toString(), submitUrl,
			"browser API submit URL", RESPONSE_ENV_KEY);

		logSuccess("Submitted mock wallet response to browser API submit URL",
			args("submit_url", submitUrl, "protocol", protocol, "encrypted", encrypted));

		return env;
	}

}

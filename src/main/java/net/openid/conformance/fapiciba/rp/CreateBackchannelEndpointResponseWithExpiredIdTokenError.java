
package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;

public class CreateBackchannelEndpointResponseWithExpiredIdTokenError extends AbstractCreateBackchannelEndpointResponseWithError {

	@Override
	protected void addError(JsonObject backchannelResponse) {
		backchannelResponse.addProperty("error", "expired_id_token_hint");
		backchannelResponse.addProperty(
			"error_description",
			"This test simulates an expired id_token in the id_token_hint parameter, " +
				"even if the id_token_hint that was passed to the test was actually valid."
		);
		log("Backchannel responds with expired_id_token_hint");
	}
}

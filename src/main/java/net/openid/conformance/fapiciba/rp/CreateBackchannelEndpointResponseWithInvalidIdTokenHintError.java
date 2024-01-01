
package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;

public class CreateBackchannelEndpointResponseWithInvalidIdTokenHintError extends AbstractCreateBackchannelEndpointResponseWithError {

	@Override
	protected void addError(JsonObject backchannelResponse) {
		backchannelResponse.addProperty("error", "invalid_id_token_hint");
		backchannelResponse.addProperty(
			"error_description",
			"This test simulates an invalid id_token in the id_token_hint parameter, " +
				"even if the id_token_hint that was passed to the test was actually valid."
		);
		log("Backchannel responds with invalid_id_token_hint");
	}
}

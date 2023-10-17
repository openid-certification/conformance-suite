
package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;

public class CreateBackchannelEndpointResponseWithUnknownUserIdError extends AbstractCreateBackchannelEndpointResponseWithError {

	@Override
	protected void addError(JsonObject backchannelResponse) {
		backchannelResponse.addProperty("error", "unknown_user_id");
		backchannelResponse.addProperty(
			"error_description",
			"This test simulates that the user represented by the id_token_hint is unknown, " +
				"resulting in an unknown_user_id error response from the backchannel endpoint.."
		);
		log("Backchannel responds with unknown_user_id");
	}
}

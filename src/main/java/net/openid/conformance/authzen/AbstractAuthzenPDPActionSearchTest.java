package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenActionSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToActionSearchEndpoint;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPActionSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenActionSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToActionSearchEndpoint.class);
	}


}

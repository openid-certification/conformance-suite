package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenResourceSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToResourceSearchEndpoint;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPResourceSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenResourceSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToResourceSearchEndpoint.class);
	}


}

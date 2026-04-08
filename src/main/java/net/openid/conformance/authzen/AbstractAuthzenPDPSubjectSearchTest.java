package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenSubjectSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToSubjectSearchEndpoint;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPSubjectSearchTest extends AbstractAuthzenPDPSearchTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenSubjectSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToSubjectSearchEndpoint.class);
	}


}

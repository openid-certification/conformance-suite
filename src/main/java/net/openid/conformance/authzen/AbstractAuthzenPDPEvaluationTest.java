package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureAuthzenDecisionResponseValMatchesExpected;
import net.openid.conformance.authzen.condition.EnsureValidDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenDecisionExpectedResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationEndpoint;
import net.openid.conformance.authzen.condition.WarnAuthzenDecisionUnknownMembers;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.access_evaluation_endpoint"
})
public abstract class AbstractAuthzenPDPEvaluationTest extends AbstractAuthzenPDPTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenEvaluationApiRequestSteps(request.getAsJsonObject("subject"), request.getAsJsonObject("resource"), request.getAsJsonObject("action"), request.getAsJsonObject("context"));
	}

	/**
	 * Override to supply the expected decision response JSON (e.g. {"decision": true}), which
	 * the decision in a 200 response is compared against. Modules that expect only an error
	 * status never validate a response body, so they need not override it.
	 */
	protected String getExpectedDecisionResponseJson() {
		return null;
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		String expectedJson = getExpectedDecisionResponseJson();
		if (expectedJson == null) {
			throw new TestFailureException(getId(), getName() + " accepts HTTP 200 but defines no expected decision");
		}
		callAndContinueOnFailure(new ExtractAuthzenDecisionExpectedResponse(expectedJson), ConditionResult.FAILURE);
		// The expected decision comes from the certification fixture (scenario 4.4), not from the
		// Authorization API specification, and the scenario has no requirement link prefix.
		callAndContinueOnFailure(EnsureAuthzenDecisionResponseValMatchesExpected.class, ConditionResult.FAILURE);
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationEndpoint.class);
	}

	@Override
	protected String getEndpointMetadataParameter() {
		return "access_evaluation_endpoint";
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-6.2");
		callAndStopOnFailure(EnsureValidDecisionResponse.class, "AUTHZEN-6.2", "AUTHZEN-5.5");
		callAndContinueOnFailure(WarnAuthzenDecisionUnknownMembers.class, ConditionResult.WARNING, "AUTHZEN-5.5", "AUTHZEN-6.2");
	}


}

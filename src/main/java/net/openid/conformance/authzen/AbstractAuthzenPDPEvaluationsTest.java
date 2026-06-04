package net.openid.conformance.authzen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationsApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureAuthzenEvaluationsResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix;
import net.openid.conformance.authzen.condition.EnsureEvaluationsResponseLengthMatchesRequest;
import net.openid.conformance.authzen.condition.EnsureNoTopLevelDecisionWhenEvaluationsPresent;
import net.openid.conformance.authzen.condition.EnsureValidEvaluationsResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointEvaluationsResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenEvaluationsExpectedResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationsEndpoint;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.access_evaluations_endpoint"
})

public abstract class AbstractAuthzenPDPEvaluationsTest extends AbstractAuthzenPDPTest {

	private String cachedEvaluationsSemantic;

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenEvaluationsApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonArray("evaluations"),
			request.getAsJsonObject("options"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationsEndpoint.class);
	}

	protected abstract String getExpectedEvaluationsResponseJson();

	/**
	 * Returns the `evaluations_semantic` value carried in the request payload, or
	 * `execute_all` when no value is set. Short-circuit semantics
	 * (`deny_on_first_deny`, `permit_on_first_permit`) MAY cause the PDP to
	 * truncate the response, which changes how the response is validated.
	 *
	 * <p>Cached after first call so idempotency loops do not re-parse the
	 * payload on every iteration.
	 */
	protected String getEvaluationsSemantic() {
		if (cachedEvaluationsSemantic != null) {
			return cachedEvaluationsSemantic;
		}
		JsonObject options = parseRequest().getAsJsonObject("options");
		String semantic = "execute_all";
		if (options != null && options.has("evaluations_semantic")) {
			JsonElement value = options.get("evaluations_semantic");
			if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
				semantic = OIDFJSON.getString(value);
			}
		}
		cachedEvaluationsSemantic = semantic;
		return semantic;
	}

	private boolean isShortCircuitSemantic() {
		String semantic = getEvaluationsSemantic();
		return "deny_on_first_deny".equals(semantic) || "permit_on_first_permit".equals(semantic);
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(new ExtractAuthzenEvaluationsExpectedResponse(getExpectedEvaluationsResponseJson()), ConditionResult.FAILURE);
		if (isShortCircuitSemantic()) {
			callAndContinueOnFailure(EnsureAuthzenEvaluationsResponseValsMatchExpectedValsAsPrefix.class, ConditionResult.FAILURE, "AUTHZEN-7.2");
		} else {
			callAndContinueOnFailure(EnsureAuthzenEvaluationsResponseValsMatchExpectedVals.class, ConditionResult.FAILURE, "AUTHZEN-7.2");
		}
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointEvaluationsResponse.class, "AUTHZEN-7.2");
		callAndStopOnFailure(EnsureValidEvaluationsResponse.class, "AUTHZEN-7.2");
		if (!isShortCircuitSemantic()) {
			callAndContinueOnFailure(EnsureEvaluationsResponseLengthMatchesRequest.class, ConditionResult.FAILURE, "AUTHZEN-7.2");
		}
		callAndContinueOnFailure(EnsureNoTopLevelDecisionWhenEvaluationsPresent.class, ConditionResult.WARNING, "AUTHZEN-7.2");
	}

}

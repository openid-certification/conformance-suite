package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddNbfToRequestObject;
import net.openid.conformance.condition.client.AddNbfValueIs8SecondsInFutureToRequestObject;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckPAREndpointResponse201WithNoError;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2AuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-request-object-with-nbf-8-seconds-in-the-future-is-accepted",
	displayName = "FAPI2-Security-Profile-ID2: ensure request object with nbf 8 seconds in the future is accepted",
	summary = "This test ensures the Authorization Server accepts request objects with a nbf claim slightly in the future (e.g., 8 seconds) to account for clock skew. It verifies robustness against minor time discrepancies, avoiding unnecessary request rejections.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPI2AuthRequestMethod.class, values = { "unsigned" })
public class FAPI2SPID2EnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestObjectSteps() {
		return super.makeCreateAuthorizationRequestObjectSteps()
			.replace(AddNbfToRequestObject.class,
				condition(AddNbfValueIs8SecondsInFutureToRequestObject.class)
					.requirement("FAPI2-5.3.2.1"));
	}

	@Override
	protected void processParResponse() {

		// if response code is not 201 then skip test
		Integer status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status");
		if (status != HttpStatus.CREATED.value()) {
			callAndContinueOnFailure(CheckPAREndpointResponse201WithNoError.class, Condition.ConditionResult.WARNING,"PAR-2.2", "PAR-2.3");
			eventLog.log(executionManager.getTestId(), "PAR endpoint doesn't seem to support clock skew, finishing test prematurely.");
			fireTestFinished();
			return;
		}

		super.processParResponse();
	}
}

package net.openid.conformance.openinsurance.testmodule.deprecated.consent;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.SetBadScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas400;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-bad-scope-test-v1",
	displayName = "Validate that requests with bad scopes return HTTP 400",
	summary = "Validate that requests with bad scopes return HTTP 400",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinConsentApiBadScopeTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		runInBlock("Validating create bad scope response v1", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)
				.replace(SetConsentsScopeOnTokenEndpointRequest.class, condition(SetBadScopeOnTokenEndpointRequest.class))));
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});


	}
}

package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

import net.openid.conformance.sequence.ConditionSequence;

@PublishTestModule(
	testName = "consent-api-bad-scope-test",
	displayName = "Validate that requests with bad scopes return HTTP 400",
	summary = "Validate that requests with bad scopes return HTTP 400",
	profile = OBBProfile.OBB_PROFILE,
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
public class ConsentApiBadScopeTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {

		runInBlock("Validating create bad scope response", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)
				.replace(SetConsentsScopeOnTokenEndpointRequest.class, condition(SetBadScopeOnTokenEndpointRequest.class))));
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas400.class);
		});


	}
}

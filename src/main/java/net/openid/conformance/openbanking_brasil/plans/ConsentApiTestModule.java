package net.openid.conformance.openbanking_brasil.plans;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddMtlsEndpointAliasesToServerConfiguration;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.*;

@PublishTestModule(
	testName = "consent-api-test",
	displayName = "Validate structure of all consent API resources",
	summary = "Validates the structure of all consent API resources",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class ConsentApiTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		eventLog.startBlock("Validating create consent response");
		callAndContinueOnFailure(CreateNewConsentValidator.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();

		callAndStopOnFailure(ConsentSelector.class);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		eventLog.startBlock("Validating get consent response");
		callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidator.class, Condition.ConditionResult.FAILURE);
		eventLog.endBlock();

		eventLog.startBlock("Deleting consent");
		callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareToFetchConsentRequest.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateHttpResponseWas404.class);
		eventLog.endBlock();


	}

}

package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.consent.CreateNewConsentValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-client-limits",
	displayName = "Validate that clients cannot obtain one another's consents",
	summary = "Validate that clients cannot obtain one another's consents",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class ConsentsApiCrossClientTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validating create consent response", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating get consent response", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Attempt to fetch with second client", () -> {
			switchToSecondClient();
			call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)
				.replace(GetStaticClientConfiguration.class, condition(GetStaticClient2Configuration.class))
				.replace(ExtractMTLSCertificatesFromConfiguration.class, condition(ExtractMTLSCertificates2FromConfiguration.class))));
			callAndStopOnFailure(GetResourceEndpointConfiguration.class);
			callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
			callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseFromConsentApiWas403.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ClearResponseFromEnvironment.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Attempt to delete with second client", () -> {
			callAndStopOnFailure(PrepareToDeleteConsent.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseFromConsentApiWas403.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Deleting consent with first client", () -> {
			switchToFirstClient();
			call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)));
			callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndStopOnFailure(RequireResponseBody.class);
			callAndStopOnFailure(SetResponseBodyOptional.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ConsentWasRejectedOrDeleted.class);
		});

	}

	private void switchToSecondClient() {
		eventLog.log(getName(),"Switching to second client to try and fetch consent");
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
	}

	private void switchToFirstClient() {
		eventLog.log(getName(),"Switching back to first client to clean up");
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		env.unmapKey("mutual_tls_authentication");
	}

}

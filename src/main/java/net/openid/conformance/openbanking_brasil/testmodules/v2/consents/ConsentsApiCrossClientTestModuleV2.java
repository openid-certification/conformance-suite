package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddConsentScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallConsentApiWithBearerToken;
import net.openid.conformance.openbanking_brasil.testmodules.support.ClearErrorResponseFromEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.ConsentIdExtractor;
import net.openid.conformance.openbanking_brasil.testmodules.support.ConsentWasRejectedOrDeleted;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseFromConsentApiWas403;
import net.openid.conformance.openbanking_brasil.testmodules.support.IgnoreResponseError;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToDeleteConsent;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToFetchConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToPostConsentRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.RequireResponseBody;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetContentTypeApplicationJson;
import net.openid.conformance.openbanking_brasil.testmodules.support.SetResponseBodyOptional;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-test-client-limits-v2",
	displayName = "Validate that clients cannot obtain one another's consents",
	summary = "Validates that clients cannot obtain one another's consents\n" +
		"\u2022 Creates a Consent V2 with all of the existing permissions \n" +
		"\u2022 Calls the GET Consents V2 with the Consent ID that has been created\n" +
		"\u2022 Checks all of the fields sent on the consent API V2 are specification compliant\n" +
		"\u2022 Calls the Token endpoint using the 2nd client provided on the configuration file\n" +
		"\u2022 Calls the GET Consents V2 with the first Consent ID created\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents V2 with the first Consent ID created, using the 2nd client\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents V2 with the first Consent ID created, using the 1st client\n" +
		"\u2022 Expects success on the Delete 20x\n" +
		"\u2022 Calls the GET Consents V2 with the 1st Consent ID created\n" +
		"\u2022 Confirms that the Consent has been sent to a Rejected state",
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
		"resource.brazilCpf",
		"consent.productType"
	}
)
public class ConsentsApiCrossClientTestModuleV2 extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validating create consent response V2", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating get consent response V2", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidatorV2.class, Condition.ConditionResult.FAILURE);
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
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseFromConsentApiWas403.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ClearErrorResponseFromEnvironment.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Attempt to delete with second client", () -> {
			callAndStopOnFailure(PrepareToDeleteConsent.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseFromConsentApiWas403.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Deleting consent with first client", () -> {
			switchToFirstClient();
			call(sequence(() -> createGetAccessTokenWithClientCredentialsSequence(clientAuthSequence)));
			callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
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

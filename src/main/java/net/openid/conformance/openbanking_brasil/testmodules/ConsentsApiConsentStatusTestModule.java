package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllResourceRelatedConsentsForHappyPathTest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-status-test",
	displayName = "Validate that consents are actually authorised on redirect",
	summary = "Validate that consents are actually authorised on redirect",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.customerUrl"
	}
)
public class ConsentsApiConsentStatusTestModule extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {
		String responseError = env.getString("resource_endpoint_error_code");
		if (Strings.isNullOrEmpty(responseError)) {
			runInBlock("Validating get consent response", () -> {
				callAndStopOnFailure(PrepareToFetchConsentRequest.class);
				callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
				call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
				preCallProtectedResource("Fetch consent");
				callAndStopOnFailure(EnsureConsentWasAuthorised.class);
			});
		} else {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
			String logMessage = String.format("Call personal endpoint");
			runInBlock(logMessage, () -> {
				callAndStopOnFailure(PrepareToCallCustomerDataEndpoint.class);
				callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
				callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.WARNING);
			});
		}
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

}

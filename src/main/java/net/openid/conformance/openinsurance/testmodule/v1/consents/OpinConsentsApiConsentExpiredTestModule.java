package net.openid.conformance.openinsurance.testmodule.v1.consents;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.CopyFromConsentUrlToResourceUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddExpirationInOneMinute;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckAuthorizationEndpointHasError;
import net.openid.conformance.openbanking_brasil.testmodules.support.ChuckWarning;
import net.openid.conformance.openbanking_brasil.testmodules.support.ResourceEndpointResponseFromFullResponse;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.ConsentHasExpiredInsteadOfBeenRejected;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-consent-api-expired-consent-test",
	displayName = "Validate that consents can expire",
	summary = "Consent will be created with a 1-minute expiry, and the user will be sent to the authorization endpoint after the consent has expired. The authorization server must return the browser to the redirect URL with a valid OAuth2 error response.\n" +
		"\u2022 Call the POST Consents API with either the customer’s business or the customer’s personal PERMISSIONS, depending on what option has been selected by the user on the configuration field\n" +
		"\u2022 Expect a 201 - Validate all of the fields of response_body of the POST Consents API response\n" +
		"\u2022 Expects a valid consent creation 201\n" +
		"\u2022 Redirects the User - He should not accept the consent, waiting for the consent to reach an expired state\n" +
		"\u2022 Verifies on the authorization endpoint response if the expiration of the consent resulted in an error message",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
	}
)
public class OpinConsentsApiConsentExpiredTestModule extends AbstractOBBrasilFunctionalTestModule {

	OpinConsentPermissionsBuilder permissionsBuilder;

	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(CopyFromConsentUrlToResourceUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.ALL);

		String productType = env.getString("config", "consent.productType");
		if (!Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL);
		}
		if (!Strings.isNullOrEmpty(productType) && productType.equals("personal")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
		}

		permissionsBuilder.build();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {

		OpenBankingBrazilPreAuthorizationSteps steps = new OpenBankingBrazilPreAuthorizationSteps(false,
			false, addTokenEndpointClientAuthentication, false, false);
		steps.replace(FAPIBrazilAddExpirationToConsentRequest.class, condition(AddExpirationInOneMinute.class));
		steps.then(exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full"),
			condition(ResourceEndpointResponseFromFullResponse.class),
			condition(OpinCreateNewConsentValidatorV1.class).dontStopOnFailure());
		return steps;
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();
		callAndContinueOnFailure(WaitFor2Seconds.class);
		callAndContinueOnFailure(WaitFor60Seconds.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		callAndStopOnFailure(ConsentHasExpiredInsteadOfBeenRejected.class);
		callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);

		fireTestFinished();
	}


	@Override
	protected void validateResponse() {

	}

}

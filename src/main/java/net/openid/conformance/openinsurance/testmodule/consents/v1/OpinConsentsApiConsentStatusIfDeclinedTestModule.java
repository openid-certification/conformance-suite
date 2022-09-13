package net.openid.conformance.openinsurance.testmodule.consents.v1;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.CopyFromConsentUrlToResourceUrl;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-consent-api-status-declined-test",
	displayName = "Validate that consents are rejected on decline",
	summary = "Validates that consents are rejected on the decline\n" +
		"\u2022 Call the POST Consents API with either the customer’s business or the customer’s personal PERMISSIONS, depending on what option has been selected by the user on the configuration field\n" +
		"\u2022 Expect a 201 - Validate all of the fields of response_body of the POST Consents API response\n" +
		"\u2022 Redirects the User to Reject the Consent\n" +
		"\u2022 Checks if rejection resulted in an error message on the redirect\n" +
		"\u2022 Calls the GET Consents endpoint\n" +
		"\u2022 Expects a 200 with the Consent being on a rejected status",
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
public class OpinConsentsApiConsentStatusIfDeclinedTestModule extends AbstractOBBrasilFunctionalTestModule {

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
		steps.then(exec().mapKey("resource_endpoint_response_full", "consent_endpoint_response_full"),
			condition(ResourceEndpointResponseFromFullResponse.class),
			condition(OpinCreateNewConsentValidatorV1.class).dontStopOnFailure());
		return steps;
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating get consent response", () -> {
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
			call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
			preCallProtectedResource("Fetch consent");

			callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		});
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();

		fireTestFinished();
	}

}

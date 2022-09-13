package net.openid.conformance.openinsurance.testmodule.consents.v1;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinConsentDetailsIdentifiedByConsentIdValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-consent-api-test-client-limits",
	displayName = "Validate that clients cannot obtain one another's consents",
	summary = "Validates that clients cannot obtain one another's consents\n" +
		"\u2022 Confirm if we are already selecting it to send either business or personal permissions \n" +
		"\u2022 If the Consent is for Business Account, call the POST consent APIs with permission CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ, CUSTOMERS_BUSINESS_QUALIFICATION_READ, and CUSTOMERS_BUSINESS_ADITTIONALINFO_READ  \n" +
		"\u2022 If the Consent is for Personal Account, call the POST consent APIs with permission CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ, CUSTOMERS_PERSONAL_QUALIFICATION_READ, and CUSTOMERS_PERSONAL_ADITTIONALINFO_READ  \n" +
		"\u2022 Creates Consent with all of the Business/ Personal, besides all existing additional permissions \n" +
		"\u2022 Calls the GET Consents with the Consent ID that has been created\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls the Token endpoint using the 2nd client provided on the configuration file\n" +
		"\u2022 Calls the GET Consents with the first Consent ID created\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents with the first Consent ID created, using the 2nd client\n" +
		"\u2022 Expects the test to return a 403 - Forbidden\n" +
		"\u2022 Calls the DELETE Consents with the first Consent ID created, using the 1st client\n" +
		"\u2022 Expects success on the Delete 204\n" +
		"\u2022 Calls the GET Consents with the 1st Consent ID created\n" +
		"\u2022 Confirms that the Consent has been sent to a Rejected state",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
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
public class OpinConsentsApiCrossClientTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	private OpinConsentPermissionsBuilder permissionsBuilder;
	@Override
	protected void runTests() {
		permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.ALL);

		String productType = env.getString("config", "consent.productType");
		if (!Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL);
		}
		if (!Strings.isNullOrEmpty(productType) && productType.equals("personal")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
		}

		runInBlock("Validating create consent response", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating get consent response v1", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinConsentDetailsIdentifiedByConsentIdValidatorV1.class, Condition.ConditionResult.FAILURE);
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

package net.openid.conformance.openinsurance.testmodule.consents.v1;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckItemCountHasMin1;
import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinConsentDetailsIdentifiedByConsentIdValidatorV1;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinCreateNewConsentValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;

@PublishTestModule(
	testName = "opin-consent-api-test",
	displayName = "Validate the structure of all consent API resources V1",
	summary = "Validates the structure of all consent API resources V1\n" +
		"\u2022 Confirm if we are already selecting it to send either business or personal permissions \n" +
		"\u2022 If the Consent is for Business Account, call the POST consent APIs with permission CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ, CUSTOMERS_BUSINESS_QUALIFICATION_READ, and CUSTOMERS_BUSINESS_ADITTIONALINFO_READ, plus all aditional permissions \n" +
		"\u2022 If the Consent is for Personal Account, call the POST consent APIs with permission CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ, CUSTOMERS_PERSONAL_QUALIFICATION_READ, and CUSTOMERS_PERSONAL_ADITTIONALINFO_READ, plus all aditional permissions  \n" +
		"\u2022 Creates Consent with all of the Business/ Personal, plus all existing additional permissions \n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls the GET Consents with the Consent ID that has been created\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n"+
		"\u2022 Calls the DELETE Consents with the Consent ID created\n" +
		"\u2022 Expects the test to return a 204 - No Content\n" +
		"\u2022 Calls the GET Consents with the Consent ID created\n" +
		"\u2022 Confirms that the Consent has been sent to a Rejected state",
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
		"consent.productType"
	}
)

public class OpinConsentApiTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

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
		permissionsBuilder.build();

		runInBlock("Validating create consent response", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(AddConsentScope.class);
			callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
			callAndStopOnFailure(SetContentTypeApplicationJson.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCreateNewConsentValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(CheckItemCountHasMin1.class);
		});

		runInBlock("Validating get consent response", () -> {
			callAndStopOnFailure(ConsentIdExtractor.class);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinConsentDetailsIdentifiedByConsentIdValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
		});

		runInBlock("Deleting consent", () -> {
			callAndContinueOnFailure(PrepareToDeleteConsent.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(IgnoreResponseError.class);
			callAndStopOnFailure(SetResponseBodyOptional.class);
			callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ConsentWasRejectedOrDeleted.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.WARNING);
		});
	}
}

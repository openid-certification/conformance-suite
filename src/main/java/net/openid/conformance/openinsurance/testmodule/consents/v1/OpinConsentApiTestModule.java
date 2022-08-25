package net.openid.conformance.openinsurance.testmodule.consents.v1;

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
		"\u2022 Creates a Consent with alSet l of the existing permissions.\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n",
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
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.ALL).build();

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

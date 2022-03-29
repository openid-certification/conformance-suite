package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "patch-consents-api-pixscheduling-revoke-authorized",
	displayName = "Patch Consents API Test Module",
	summary = "WIP",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"client.org_jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilPatchPaymentConsent",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingPatchShouldNotBeUsedOnAuthorisedConsent extends AbstractFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void requestProtectedResource() {
		callAndStopOnFailure(SaveInitialRefreshToken.class);
		call(verifyRefreshTokenRotationIsDisabled());
		call(verifyRefreshTokenRotationIsDisabled());
		callAndStopOnFailure(PaymentConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToPatchConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
		callAndStopOnFailure(SetPatchConsentsRevokedAndRevokedByTPP.class);
		call(new SignedPaymentConsentSequence()
			.replace(EnsureHttpStatusCodeIs201.class,condition(EnsureConsentResponseCodeWas422.class))
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
			.insertBefore(EnsureHttpStatusCodeIs201.class,condition(EnsurePatchPayment422ResponseCodeIsOperationNotAllowed.class))
		);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class))
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence());
		return preauthSteps;
	}

	private ConditionSequence paymentConsentEditingSequence() {
		return sequenceOf(
			condition(FAPIBrazilCreatePaymentConsentRequest.class),
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(RemovePaymentDateFromConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTomorrow.class)
		);
	}

	private ConditionSequence verifyRefreshTokenRotationIsDisabled(){
		return sequenceOf(
			condition(GenerateRefreshTokenRequest.class),
			condition(SetPaymentsScopeOnTokenEndpointRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class),
			condition(SignClientAuthenticationAssertion.class),
			condition(AddClientAssertionToTokenEndpointRequest.class),
			condition(CallTokenEndpoint.class),
			condition(EnsureRefreshTokenHasNotRotated.class)
		);
	}

	@Override
	protected void validateResponse() {}
}

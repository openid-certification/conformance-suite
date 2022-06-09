package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "patch-consents-api-pixscheduling-revoke-authorized",
	displayName = "Patch Consents API Test Module",
	summary = "This test is an unhappy path PATCH consents test module.\n\n" +
		"Flow:\n" +
		"Creates a payment consent scheduled for today + 1 day, re-direct the user to authorize the consent, calls the token endpoint multiple times to validate refresh tokens are not being rotated, attempts to PATCH the consent with status REVOKED and revokedBy TPP, the test is expecting a 422 error being returned with the code OPERACAO_NAO_PERMITIDA_STATUS \n\n" +
		"Required:\n" +
		"Consent url pointing at the consent endpoint.\n",
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
		"resource.brazilPaymentConsent",
		"resource.brazilPatchPaymentConsent",
		"resource.brazilOrganizationId"
	}
)
public class PixSchedulingPatchShouldNotBeUsedOnAuthorisedConsent extends AbstractFunctionalTestModule {

	protected ClientAuthType clientAuthType;

	@Override
	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		clientAuthType = getVariant(ClientAuthType.class);
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock("Calling Token Endpoint and validating refresh token is not rotated");
		callAndStopOnFailure(SaveInitialRefreshToken.class);
		call(verifyRefreshTokenRotationIsDisabled());
		call(verifyRefreshTokenRotationIsDisabled());
		callAndStopOnFailure(UpdateAccessTokenAfterCallingTokenEndpoint.class);
		eventLog.startBlock("Attempting to PATCH consents");
		callAndStopOnFailure(PaymentConsentIdExtractor.class);
		callAndStopOnFailure(PrepareToPatchConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilGeneratePatchPaymentConsentRequest.class);
		callAndStopOnFailure(SetPatchConsentsRevokedAndRevokedByTPP.class);
		call(new SignedPaymentConsentSequence()
			.replace(EnsureHttpStatusCodeIs201.class,condition(EnsureConsentResponseCodeWas422.class))
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIPatchConsentsRequest.class))
			.replace(AddAudAsPaymentConsentUriToRequestObject.class, condition(AddAudToPatchConsentRequest.class))
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
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(RemovePaymentDateFromConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTomorrow.class)
		);
	}

	private ConditionSequence verifyRefreshTokenRotationIsDisabled(){
		ConditionSequence sequence = sequenceOf(
			condition(GenerateRefreshTokenRequest.class),
			condition(SetPaymentsScopeOnTokenEndpointRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class),
			condition(SignClientAuthenticationAssertion.class),
			condition(AddClientAssertionToTokenEndpointRequest.class),
			condition(CallTokenEndpoint.class),
			condition(EnsureRefreshTokenHasNotRotated.class)
		);
		if(clientAuthType == ClientAuthType.MTLS){
			sequence.insertAfter(SetPaymentsScopeOnTokenEndpointRequest.class, condition(AddClientIdToTokenEndpointRequest.class))
				.skip(CreateClientAuthenticationAssertionClaims.class, "Skipping step for MTLS")
				.skip(SignClientAuthenticationAssertion.class, "Skipping step for MTLS")
				.skip(AddClientAssertionToTokenEndpointRequest.class, "Skipping step for MTLS");
		}
		return sequence;
	}

	@Override
	protected void validateResponse() {}
}

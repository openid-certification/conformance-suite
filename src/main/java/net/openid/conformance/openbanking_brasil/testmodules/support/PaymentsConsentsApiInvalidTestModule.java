package net.openid.conformance.openbanking_brasil.testmodules.support;


import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-invalid-test",
	displayName = "Payments Consents API invalid test module",
	summary = "Payments Consents API invalid test module" +
		"Flow:" +
		"Makes a series of bad consent flows with set of bad values - expects 422 for each." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Config: We manually set the values for payment currency and the payment amount.",
	profile = OBBProfile.OBB_PROFILE,
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
public class PaymentsConsentsApiInvalidTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		eventLog.startBlock("Preparing a payment consent request");
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
		eventLog.startBlock("Setting payment to be invalid");
		callAndStopOnFailure(SetPaymentCurrency.class);
		eventLog.startBlock("Attempting to make a consent ");
		attemptConsentCreation();
	}

	protected void attemptConsentCreation() {
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateIdempotencyKey.class);
		callAndStopOnFailure(AddIdempotencyKeyHeader.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		callAndStopOnFailure(FAPIBrazilExtractClientMTLSCertificateSubject.class);
		call(exec().mapKey("request_object_claims", "consent_endpoint_request"));
		callAndStopOnFailure(AddAudAsPaymentConsentUriToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
		call(exec().unmapKey("request_object_claims"));
		eventLog.startBlock("Make payment consent request");
		callAndStopOnFailure(FAPIBrazilSignPaymentConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, Condition.ConditionResult.FAILURE);
		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
		eventLog.startBlock("Validate response");
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(EnsureConsentResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
	}
}

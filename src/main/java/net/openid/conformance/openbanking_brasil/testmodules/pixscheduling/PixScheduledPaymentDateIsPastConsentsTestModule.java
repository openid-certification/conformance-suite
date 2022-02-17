package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractClientCredentialsGrantFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-in-past",
	displayName = "Pix scheduled payment date in past",
	summary = "Attempts to create a payment consent scheduled for a day in the future, with a payment date of tomorrow, and expects a 422 response with the error NAO_INFORMADIO",
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
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilOrganizationId"
	}
)
public class PixScheduledPaymentDateIsPastConsentsTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndContinueOnFailure(SanitiseQrCodeConfig.class);
	}

	@Override
	protected void runTests() {
		eventLog.startBlock("Setting date to yesterday");
		callAndStopOnFailure(EnsurePixScheduleDateIsInPast.class);
		eventLog.startBlock("Preparing a payment consent request");
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);
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
		callAndStopOnFailure(Ensure422ResponseCodeWasDETALHE_PGTO_INVALIDOConsent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidatePaymentConsentErrorResponse422.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
	}
}

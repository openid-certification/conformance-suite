package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-idempotency-test",
	displayName = "Payments API test module for re-using idempotency keys",
	summary = "Payments API test module for re-using idempotency keys",
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
		"resource.resourceUrl"
	}
)
public class PaymentsConsentsReuseIdempotencyKeyTestModule extends AbstractOBBrasilFunctionalTestModule {

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
	protected void validateResponse() {
		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class);
		callAndStopOnFailure(ModifyPixPaymentValue.class);
		callPix();
	}

	public void callPix() {

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(AddIdempotencyKeyHeader.class);
		callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);

		call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

		callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		call(exec().unmapKey("request_object_claims"));

		callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

		callAndStopOnFailure(EnsureResponseCodeWas422.class);
	}
}

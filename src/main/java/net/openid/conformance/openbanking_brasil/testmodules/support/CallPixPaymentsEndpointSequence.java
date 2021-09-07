package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.fapi1advancedfinal.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallPixPaymentsEndpointSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-3");

		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-4");

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		call(sequenceOf(condition(CreateIdempotencyKey.class), condition(AddIdempotencyKeyHeader.class)));
		callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(SetResourceMethodToPost.class);
		callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);

		// we reuse the request object conditions to add various jwt claims; it would perhaps make sense to make
		// these more generic.
		call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

		// aud (in the JWT request): the Resource Provider (eg the institution holding the account) must validate if the value of the aud field matches the endpoint being triggered;
		callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

		//iss (in the JWT request and in the JWT response): the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender;
		callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

		//jti (in the JWT request and in the JWT response): the value of the jti field shall be filled with the UUID defined by the institution according to [RFC4122] version 4;
		callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

		//iat (in the JWT request and in the JWT response): the iat field shall be filled with the message generation time and according to the standard established in [RFC7519](https:// datatracker.ietf.org/doc/html/rfc7519#section-2) to the NumericDate format.
		callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

		call(exec().unmapKey("request_object_claims"));

		callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);

		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

	}

}

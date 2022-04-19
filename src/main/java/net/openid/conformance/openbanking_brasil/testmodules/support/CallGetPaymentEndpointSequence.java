package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.AddJWTAcceptHeaderRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CallGetPaymentEndpointSequence extends AbstractConditionSequence {

	@Override
	public void evaluate(){

		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);

		callAndStopOnFailure(SetProtectedResourceUrlToSelfEndpoint.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);

		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);

		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);

		callAndStopOnFailure(CreateIdempotencyKey.class);
		callAndStopOnFailure(AddIdempotencyKeyHeader.class);

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);

		callAndStopOnFailure(AddJWTAcceptHeaderRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(EnsureMatchingFAPIInteractionId.class);

		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));

		callAndStopOnFailure(EnsureContentTypeApplicationJwt.class);

		callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class);

		callAndStopOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class);

		callAndStopOnFailure(FAPIBrazilValidateResourceResponseTyp.class);

		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));
		callAndStopOnFailure(FetchServerKeys.class);
		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		callAndContinueOnFailure(ValidateResourceResponseSignature.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));
	}
}

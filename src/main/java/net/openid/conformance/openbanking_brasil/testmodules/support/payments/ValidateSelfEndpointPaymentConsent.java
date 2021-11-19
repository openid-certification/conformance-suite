package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateSelfEndpointPaymentConsent extends AbstractConditionSequence {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public void evaluate() {
		callAndStopOnFailure(SaveOldValues.class);
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSelfEndpoint.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(CreateIdempotencyKey.class);
		callAndStopOnFailure(AddIdempotencyKeyHeader.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		callAndStopOnFailure(AddJWTAcceptHeaderRequest.class);
		callAndStopOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class);
		callAndStopOnFailure(ExtractResponseCodeFromFullResponse.class);
		callAndStopOnFailure(EnsureResponseWasJwt.class);
		callAndContinueOnFailure(ThrowWarningFor406.class, Condition.ConditionResult.WARNING);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndStopOnFailure(LoadOldValues.class);
	}
}

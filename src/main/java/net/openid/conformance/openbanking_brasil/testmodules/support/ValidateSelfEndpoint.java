package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateSelfEndpoint extends AbstractConditionSequence {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public void evaluate() {
		callAndStopOnFailure(SaveOldValues.class);
		callAndStopOnFailure(ClearRequestObjectFromEnvironment.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSelfEndpoint.class);
		callAndStopOnFailure(SetResourceMethodToGet.class);
		callAndStopOnFailure(ClearContentTypeHeaderForResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndStopOnFailure(LoadOldValues.class);
		callAndStopOnFailure(ValidateErrorAndMetaFieldNames.class);
	}
}

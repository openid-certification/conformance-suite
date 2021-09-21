package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateSelfEndpoint extends AbstractConditionSequence {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public void evaluate() {
		callAndStopOnFailure(SaveOldValues.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSelfEndpoint.class);
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
		callAndStopOnFailure(ExtractResponseCodeFromFullResponse.class);
		callAndStopOnFailure(EnsureResponseCodeWas200.class);
		callAndStopOnFailure(LoadOldValues.class);
	}
}

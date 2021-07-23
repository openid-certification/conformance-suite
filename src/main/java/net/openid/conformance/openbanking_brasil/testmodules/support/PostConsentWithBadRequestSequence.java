package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.client.FAPIBrazilAddExpirationToConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilCreateConsentRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class PostConsentWithBadRequestSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilCreateConsentRequest.class);
		callAndStopOnFailure(FAPIBrazilAddExpirationToConsentRequest.class);
		callAndContinueOnFailure(CallConsentApiWithBearerToken.class);
		callAndStopOnFailure(EnsureResponseCodeWas400.class);
	}

}

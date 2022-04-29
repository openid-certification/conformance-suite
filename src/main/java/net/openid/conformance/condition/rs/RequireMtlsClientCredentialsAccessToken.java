package net.openid.conformance.condition.rs;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class RequireMtlsClientCredentialsAccessToken extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);
	}
}

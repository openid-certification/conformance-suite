package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingBrazilDynamicClientRegistrationKeyPublication extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(FAPIBrazilExtractJwksUriFromSoftwareStatement.class, "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(AddJwksUriToDynamicRegistrationRequest.class,
			"RFC7591-2", "BrazilOBDCR-7.1-5");
		callAndStopOnFailure(FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest.class,
			"BrazilOBDCR-7.1-6");
		callAndStopOnFailure(AddSoftwareStatementToDynamicRegistrationRequest.class);
	}
}

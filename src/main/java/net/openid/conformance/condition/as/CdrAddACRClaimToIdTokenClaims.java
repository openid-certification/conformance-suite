package net.openid.conformance.condition.as;

import java.util.List;

public class CdrAddACRClaimToIdTokenClaims extends AbstractBrazilAddACRClaimToIdTokenClaims {

	@Override
	protected List<String> getAcceptableAcrValues() {
		// higher level of assurance preferred when the client requested both
		return List.of("urn:cds.au:cdr:3", "urn:cds.au:cdr:2");
	}

}

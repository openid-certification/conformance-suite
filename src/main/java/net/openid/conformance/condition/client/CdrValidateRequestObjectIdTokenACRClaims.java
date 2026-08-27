package net.openid.conformance.condition.client;

import java.util.List;

public class CdrValidateRequestObjectIdTokenACRClaims extends FAPIValidateRequestObjectIdTokenACRClaims {

	@Override
	protected List<String> getExpectedAcrValues() {
		return List.of("urn:cds.au:cdr:3", "urn:cds.au:cdr:2");
	}

}

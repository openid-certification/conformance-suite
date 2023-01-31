package net.openid.conformance.condition.as;

import java.util.Arrays;
import java.util.List;


public class FAPIBrazilOBAddACRClaimToIdTokenClaims extends AbstractBrazilAddACRClaimToIdTokenClaims {

	@Override
	protected List<String> getAcceptableAcrValues() {
		return Arrays.asList("urn:brasil:openbanking:loa2", "urn:brasil:openbanking:loa3");
	}

}

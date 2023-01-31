package net.openid.conformance.condition.as;

import java.util.Arrays;
import java.util.List;


public class FAPIBrazilOPINAddACRClaimToIdTokenClaims extends AbstractBrazilAddACRClaimToIdTokenClaims {

	@Override
	protected List<String> getAcceptableAcrValues() {
		return Arrays.asList("urn:brasil:openinsurance:loa2", "urn:brasil:openinsurance:loa3");
	}

}

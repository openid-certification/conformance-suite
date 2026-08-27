package net.openid.conformance.condition.as;

import java.util.List;

public class CdrCheckForUnexpectedClaimsInClaimsParameter extends CheckForUnexpectedClaimsInClaimsParameter {

	@Override
	protected List<String> getExpectedClaims() {
		return List.of(
			// as per https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
			"userinfo",
			"id_token",
			// defined by the CDR standards
			"sharing_duration",
			"cdr_arrangement_id"
		);
	}

}

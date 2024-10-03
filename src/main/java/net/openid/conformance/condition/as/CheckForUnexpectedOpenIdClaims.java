package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateOpenIdStandardClaims;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckForUnexpectedOpenIdClaims extends AbstractValidateOpenIdStandardClaims {

	private List<String> standardClaimsAdditions = List.of(
		// As per https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.5.5.1.1
		"acr",
		// As per https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-financial-api-1_ID3.html#section-7.2.2-8
		// note that cpf & cnpj have been removed from the latest Brazil standard that starts in ~Apr 2024, so we can remove them once we stop issues certifications for the older profile
		"cpf",
		// As per https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-financial-api-1_ID3.html#section-7.2.2-10
		"cnpj",
		// As per https://openbanking.atlassian.net/wiki/spaces/DZ/pages/83919096/Open+Banking+Security+Profile+-+Implementer+s+Draft+v1.1.2#OpenBankingSecurityProfile-Implementer'sDraftv1.1.2-HybridGrantParameters
		"openbanking_intent_id",
		// as per https://openid.net/specs/openid-connect-4-identity-assurance-1_0-ID3.html#name-verified_claims-element
		"verified_claims"
	);

	// For success/failure result purposes we maintain allMemberClaims/invalidMemberClaims maps
	//
	// eg. allMemberClaims:
	//       id_token
	//	   given_name
	//	   ...
	//       userinfo
	//	   family_name
	//	   ...
	private void addClaimMemberToMap(String claim, String claimMember, Map<String, List<String>> map) {
		List<String> claimsList;

		if (map.containsKey(claim)) {
			claimsList = map.get(claim);
		}
		else {
			claimsList = new ArrayList<>();
		}

		claimsList.add(claimMember);
		map.put(claim, claimsList);
	}

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {

		HashMap<String, List<String>> allMemberClaims = new HashMap<>();
		HashMap<String, List<String>> unknownMemberClaims = new HashMap<>();

		JsonObject claimsParameter = env.getElementFromObject("authorization_request_object", "claims.claims").getAsJsonObject();

		if (claimsParameter == null || claimsParameter.size() == 0) {
			logSuccess("authorization_request_object.claims.claims does not exist or is empty");
			return env;
		}

		for (String claim : claimsParameter.keySet()) {
			JsonElement claimObject = claimsParameter.get(claim);

			if (claimObject instanceof JsonObject) {
				for (String member : claimObject.getAsJsonObject().keySet()) {

					addClaimMemberToMap(claim, member, allMemberClaims);

					if (STANDARD_CLAIMS.containsKey(member) || standardClaimsAdditions.contains(member)) {
						continue;
					}

					addClaimMemberToMap(claim, member, unknownMemberClaims);
				}
			}
		}

		if (unknownMemberClaims.isEmpty()) {
			logSuccess("authorization_request_object.claims.claims member objects contain only expected claims", args("claims", allMemberClaims));
		} else {
			throw error("unknown claims found in authorization_request_object.claims.claims member objects", args("claims", allMemberClaims, "unknown_claims", unknownMemberClaims));
		}

		return env;
	}
}

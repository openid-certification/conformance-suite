package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		JsonElement claimNames = env.getElementFromObject("userinfo", "_claim_names");
		JsonElement claimSources = env.getElementFromObject("userinfo", "_claim_sources");

		if (claimNames == null) {
			log("User info is not contains '_claim_names'");
		} else if (claimSources == null) {
			log("User info is not contains '_claim_sources'");
		} else {

			Set<String> memberValuesInClaimNames = new HashSet<>();

			for (String keyClaimName : claimNames.getAsJsonObject().keySet()) {
				memberValuesInClaimNames.add(OIDFJSON.getString(claimNames.getAsJsonObject().get(keyClaimName)));
			}

			for (String keyClaimSource : claimSources.getAsJsonObject().keySet()) {
				if (!memberValuesInClaimNames.contains(keyClaimSource)) {
					throw error("Member name '" + keyClaimSource + "' in '_claim_sources' is not referenced by member values in '_claim_names'", args("_claim_names", claimNames, "_claim_sources", claimSources));
				}
 			}

			logSuccess("Member names in '_claim_sources' are referenced by member values in '_claim_names'", args("_claim_names", claimNames, "_claim_sources", claimSources));
		}

		return env;
	}

}

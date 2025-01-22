package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class CompareTrustChains extends AbstractCondition {

	// Remove claims we don't want to compare:
	// - iat and exp are time based,
	// - trust_marks are signed jwts currently out of scope
	protected static final List<String> ENTITY_STATEMENT_CLAIMS_TO_NOT_COMPARE = List.of("iat", "exp", "trust_marks");

	@Override
	@PreEnvironment(required = { "trust_chains" } )
	public Environment evaluate(Environment env) {

		JsonObject trustChains = env.getObject("trust_chains").getAsJsonObject();
		JsonArray manualChain = trustChains.get("manual").getAsJsonArray();
		JsonArray resolvedChain = trustChains.get("resolved").getAsJsonArray();

		if (manualChain.size()!= resolvedChain.size()) {
			throw error("The number of entries in the trust chains does not match", args("trust_chains", trustChains));
		}

		List<String> entityStatementClaimsToCompare = new ArrayList<>(EntityUtils.STANDARD_ENTITY_STATEMENT_CLAIMS);
		entityStatementClaimsToCompare.removeAll(ENTITY_STATEMENT_CLAIMS_TO_NOT_COMPARE);

		List<String> diffs = new ArrayList<>();
		for (int i = 0; i < manualChain.size(); i++) {
			try {
				String manualEntry = OIDFJSON.getString(manualChain.get(i));
				SignedJWT manualJwt = SignedJWT.parse(manualEntry);
				JsonElement manualElm = JsonParser.parseString(manualJwt.getPayload().toString());

				String resolvedEntry = OIDFJSON.getString(resolvedChain.get(i));
				SignedJWT resolvedJwt = SignedJWT.parse(resolvedEntry);
				JsonElement resolvedElm = JsonParser.parseString(resolvedJwt.getPayload().toString());

				List<String> diff = EntityUtils.diffEntityStatements(entityStatementClaimsToCompare, manualElm, resolvedElm);
				if (!diff.isEmpty()) {
					String iss = OIDFJSON.getString(resolvedElm.getAsJsonObject().get("iss"));
					String sub = OIDFJSON.getString(resolvedElm.getAsJsonObject().get("sub"));
					String diffMessage =
						"The entity statement at index %d in the trust chain " +
							"(issued by %s for subject %s) mismatches on the following claims: %s";
					diffs.add(diffMessage.formatted(i, iss, sub, String.join(", ", diff)));
				}
			} catch (ParseException e) {
				throw error("Failed to parse at least one of the entries in the trust chains", e, args("trust_chains", trustChains));
			}
		}

		if(!diffs.isEmpty()) {
			throw error("The trust chains do not match", args("diffs", diffs, "trust_chains", trustChains));
		}

		logSuccess("Manually built trust chain matches the resolved trust chain", args("trust_chains", trustChains));
		return env;
	}
}

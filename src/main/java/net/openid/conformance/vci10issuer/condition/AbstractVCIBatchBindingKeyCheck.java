package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for conditions checking the binding keys extracted from a batch of issued
 * credentials. Keys are compared by JWK thumbprint (RFC 7638), which only covers the
 * required key parameters so extra members like kid or alg do not affect the comparison.
 */
public abstract class AbstractVCIBatchBindingKeyCheck extends AbstractCondition {

	protected List<String> getThumbprints(Environment env, String envKey, String description) {
		JsonArray keys = env.getObject(envKey).getAsJsonArray("keys");
		List<String> thumbprints = new ArrayList<>();
		for (JsonElement keyEl : keys) {
			thumbprints.add(computeThumbprint(keyEl, description));
		}
		return thumbprints;
	}

	protected String computeThumbprint(JsonElement jwkEl, String description) {
		try {
			return JWK.parse(jwkEl.getAsJsonObject().toString()).computeThumbprint().toString();
		} catch (ParseException | JOSEException | IllegalStateException e) {
			throw error("Failed to parse " + description + " as a JWK", e, args("jwk", jwkEl));
		}
	}
}

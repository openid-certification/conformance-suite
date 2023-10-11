package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class ValidateClientAssertionClaimsForBackchannelEndpoint extends ValidateClientAssertionClaims {

	@Override
	protected void validateAud(Environment env) {
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		if (Strings.isNullOrEmpty(backchannelEndpoint)) {
			throw error("Couldn't find issuer or client or token endpoint values in the test configuration to test the assertion");
		}

		String mtlsBackchannelEndpoint = env.getString("server", "mtls_endpoint_aliases.backchannel_authentication_endpoint");
		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null || aud.isJsonNull()) {
			throw error("Missing aud", args("aud", aud));
		}

		List<String> backchannelEndpoints = new ArrayList<>(List.of(backchannelEndpoint));
		if (mtlsBackchannelEndpoint != null) {
			backchannelEndpoints.add(mtlsBackchannelEndpoint);
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(backchannelEndpoint)) &&
				!aud.getAsJsonArray().contains(new JsonPrimitive(mtlsBackchannelEndpoint))) {
				throw error("aud not found", args("expected", backchannelEndpoints, "actual", aud));
			}
		} else {
			String audStr = OIDFJSON.getString(aud);
			if(!backchannelEndpoint.startsWith(audStr) && !mtlsBackchannelEndpoint.startsWith(audStr)) {
				throw error("aud mismatch", args("expected", backchannelEndpoints, "actual", aud));
			}
		}
	}}

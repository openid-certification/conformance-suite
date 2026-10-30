package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks the header fields ETSI TS 119 475 requires of a registration certificate formatted as a JWT:
 * {@code typ} is {@code rc-wrp+jwt} and {@code x5c} carries the certificate chain to verify it.
 */
public class VCIValidateIssuerInfoRegistrationCertificateHeader extends AbstractCondition {

	public static final String TYP = "rc-wrp+jwt";

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject header = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "header").getAsJsonObject();

		JsonElement typ = header.get("typ");
		if (typ == null || !typ.isJsonPrimitive() || !TYP.equals(OIDFJSON.getString(typ))) {
			throw error("The 'typ' of the registration certificate in 'issuer_info' is not '" + TYP + "'",
				args("header", header, "expected", TYP));
		}

		JsonElement x5c = header.get("x5c");
		if (x5c == null || !x5c.isJsonArray() || x5c.getAsJsonArray().isEmpty()) {
			throw error("The header of the registration certificate in 'issuer_info' has no 'x5c' with the certificate chain"
				+ " to verify it", args("header", header));
		}

		logSuccess("The registration certificate header has 'typ' '" + TYP + "' and an 'x5c' certificate chain",
			args("header", header));
		return env;
	}
}

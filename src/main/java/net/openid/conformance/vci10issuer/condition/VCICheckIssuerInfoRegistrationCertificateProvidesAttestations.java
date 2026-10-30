package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the registration certificate in {@code issuer_info}, issued to a provider of
 * attestations, has a non-empty {@code provides_attestations}, which ETSI TS 119 475 recommends for
 * such a certificate; the caller reports its absence as a warning.
 */
public class VCICheckIssuerInfoRegistrationCertificateProvidesAttestations extends AbstractCondition {

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject payload = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload").getAsJsonObject();

		JsonElement providesAttestations = payload.get("provides_attestations");
		if (providesAttestations == null || !providesAttestations.isJsonArray() || providesAttestations.getAsJsonArray().isEmpty()) {
			throw error("The registration certificate in 'issuer_info' has no non-empty 'provides_attestations' listing the"
				+ " attestations the issuer is registered to provide", args("payload", payload));
		}
		logSuccess("The registration certificate in 'issuer_info' lists the attestations the issuer is registered to provide",
			args("provides_attestations", providesAttestations));
		return env;
	}
}

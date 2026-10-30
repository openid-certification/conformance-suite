package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the registration certificate in {@code issuer_info} carries the payload fields ETSI TS
 * 119 475 maps from the register: the subject's name and legal or personal name, identifier, country,
 * registry URI, service description, privacy policy, information and support URIs, supervisory
 * authority, policy identifier, certificate policy and status. The fields are those "provided by the
 * registry", which a given register may not provide, so the caller reports a missing one as a warning.
 * {@code entitlements} and {@code iat} are checked by their own conditions.
 */
public class VCICheckIssuerInfoRegistrationCertificateRegistryFields extends AbstractCondition {

	private static final List<String> FIELDS = List.of("name", "sub", "country", "registry_uri", "srv_description",
		"privacy_policy", "info_uri", "support_uri", "supervisory_authority", "policy_id", "certificate_policy", "status");

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonObject payload = env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload").getAsJsonObject();

		List<String> missing = new ArrayList<>();
		for (String field : FIELDS) {
			if (!payload.has(field)) {
				missing.add(field);
			}
		}
		// sub_ln for a legal person, sub_gn and sub_fn for a natural person
		if (!payload.has("sub_ln") && !(payload.has("sub_gn") && payload.has("sub_fn"))) {
			missing.add("sub_ln (or sub_gn and sub_fn)");
		}

		if (!missing.isEmpty()) {
			throw error("The registration certificate in 'issuer_info' lacks payload fields a registration certificate carries"
					+ " from the register", args("missing", missing, "payload", payload));
		}
		logSuccess("The registration certificate in 'issuer_info' carries the payload fields from the register", args("payload", payload));
		return env;
	}
}

package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that every credential configuration the issuer metadata advertises is among the
 * {@code providesAttestations} of the {@code registrar_dataset} in {@code issuer_info}, which ETSI TS
 * 119 472-3 expects to list the same attestation types or a superset; the caller reports a mismatch
 * as a warning. An SD-JWT VC configuration matches by {@code vct} in {@code meta.vct_values}, an mdoc
 * one by {@code doctype} equal to {@code meta.doctype_value}; configurations of other formats are not
 * compared.
 */
public class VCIWarnIfCredentialConfigurationsNotInProvidesAttestations extends AbstractCondition {

	@Override
	@PreEnvironment(required = {VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY, "vci"})
	public Environment evaluate(Environment env) {
		JsonElement providesEl = env.getElementFromObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY, "providesAttestations");
		if (providesEl == null || !providesEl.isJsonArray()) {
			throw error("The 'registrar_dataset' in 'issuer_info' has no 'providesAttestations' array to compare the credential"
				+ " configurations with", args("registrar_dataset", env.getObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY)));
		}
		JsonArray provides = providesEl.getAsJsonArray();

		JsonElement configsEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_configurations_supported");
		if (configsEl == null || !configsEl.isJsonObject()) {
			throw error("The credential issuer metadata has no 'credential_configurations_supported' object");
		}
		JsonObject configs = configsEl.getAsJsonObject();

		List<String> notProvided = new ArrayList<>();
		List<String> notCompared = new ArrayList<>();
		for (String id : configs.keySet()) {
			JsonElement configEl = configs.get(id);
			if (!configEl.isJsonObject()) {
				continue;
			}
			JsonObject config = configEl.getAsJsonObject();
			String format = stringOrNull(config.get("format"));
			Boolean covered = null;
			if ("dc+sd-jwt".equals(format)) {
				String vct = stringOrNull(config.get("vct"));
				covered = vct != null && provides(provides, format, "vct_values", vct);
			} else if ("mso_mdoc".equals(format)) {
				String doctype = stringOrNull(config.get("doctype"));
				covered = doctype != null && provides(provides, format, "doctype_value", doctype);
			}
			if (covered == null) {
				notCompared.add(id);
			} else if (!covered) {
				notProvided.add(id);
			}
		}

		if (!notProvided.isEmpty()) {
			throw error("The issuer metadata advertises credential configurations whose attestation type is not among the"
					+ " 'providesAttestations' of the 'registrar_dataset' in 'issuer_info', the attestation types the issuer is"
					+ " registered to provide",
				args("not_provided", notProvided, "provides_attestations", provides, "not_compared", notCompared));
		}
		logSuccess("Every compared credential configuration is among the 'providesAttestations' of the 'registrar_dataset'",
			args("provides_attestations", provides, "not_compared", notCompared));
		return env;
	}

	private static boolean provides(JsonArray provides, String format, String metaMember, String value) {
		for (JsonElement element : provides) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject attestation = element.getAsJsonObject();
			if (!format.equals(stringOrNull(attestation.get("format"))) || !attestation.has("meta") || !attestation.get("meta").isJsonObject()) {
				continue;
			}
			JsonElement member = attestation.getAsJsonObject("meta").get(metaMember);
			if (member == null) {
				continue;
			}
			if (member.isJsonArray()) {
				for (JsonElement item : member.getAsJsonArray()) {
					if (value.equals(stringOrNull(item))) {
						return true;
					}
				}
			} else if (value.equals(stringOrNull(member))) {
				return true;
			}
		}
		return false;
	}

	private static String stringOrNull(JsonElement element) {
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? OIDFJSON.getString(element) : null;
	}
}

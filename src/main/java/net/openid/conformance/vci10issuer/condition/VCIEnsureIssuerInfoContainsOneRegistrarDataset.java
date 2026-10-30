package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks that exactly one element of {@code issuer_info} is a {@code registrar_dataset} whose data
 * is a non-empty JSON object, which ETSI TS 119 472-3 requires whether or not a registration
 * certificate is present, and that at most one is a {@code registration_cert}. Stores the dataset's
 * data for the checks of its members.
 */
public class VCIEnsureIssuerInfoContainsOneRegistrarDataset extends AbstractCondition {

	public static final String ENV_KEY = "issuer_info_registrar_dataset";

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY)
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement issuerInfoEl = env.getElementFromObject(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, "issuer_info");
		if (issuerInfoEl == null || !issuerInfoEl.isJsonArray()) {
			throw error("'issuer_info' is not an array", args("issuer_info", issuerInfoEl));
		}
		JsonArray issuerInfo = issuerInfoEl.getAsJsonArray();

		JsonArray datasets = elementsWithFormat(issuerInfo, "registrar_dataset");
		JsonArray certificates = elementsWithFormat(issuerInfo, "registration_cert");

		if (datasets.isEmpty()) {
			throw error("'issuer_info' has no element with format 'registrar_dataset'. The registration information provided"
					+ " by the provider's registrar must always be present, also when a registration certificate is",
				args("issuer_info", issuerInfo));
		}
		if (datasets.size() > 1) {
			throw error("'issuer_info' has more than one element with format 'registrar_dataset'",
				args("issuer_info", issuerInfo, "registrar_datasets", datasets));
		}
		if (certificates.size() > 1) {
			throw error("'issuer_info' has more than one element with format 'registration_cert'",
				args("issuer_info", issuerInfo, "registration_certs", certificates));
		}

		JsonElement data = datasets.get(0).getAsJsonObject().get("data");
		if (data == null || !data.isJsonObject() || data.getAsJsonObject().isEmpty()) {
			throw error("The 'data' of the 'registrar_dataset' element of 'issuer_info' is not a non-empty JSON object",
				args("registrar_dataset", datasets.get(0)));
		}

		env.putObject(ENV_KEY, data.getAsJsonObject());
		logSuccess("'issuer_info' contains one 'registrar_dataset' element"
				+ (certificates.isEmpty() ? " and no registration certificate" : " and one 'registration_cert' element"),
			args("registrar_dataset", data));
		return env;
	}

	static JsonArray elementsWithFormat(JsonArray issuerInfo, String format) {
		JsonArray matching = new JsonArray();
		for (JsonElement element : issuerInfo) {
			if (element.isJsonObject()) {
				JsonObject obj = element.getAsJsonObject();
				JsonElement formatEl = obj.get("format");
				if (formatEl != null && formatEl.isJsonPrimitive() && formatEl.getAsJsonPrimitive().isString()
					&& format.equals(OIDFJSON.getString(formatEl))) {
					matching.add(obj);
				}
			}
		}
		return matching;
	}
}

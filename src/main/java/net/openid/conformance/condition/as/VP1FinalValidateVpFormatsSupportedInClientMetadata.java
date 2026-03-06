package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VP1FinalValidateVpFormatsSupportedInClientMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY }, strings = "credential_format")
	public Environment evaluate(Environment env) {

		JsonElement clientMetadata = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata");
		if (clientMetadata == null || !clientMetadata.isJsonObject()) {
			throw error("client_metadata is missing or not a JSON object");
		}

		JsonElement vpFormatsSupported = clientMetadata.getAsJsonObject().get("vp_formats_supported");
		if (vpFormatsSupported == null) {
			throw error("vp_formats_supported is missing from client_metadata but is required",
				args("client_metadata", clientMetadata));
		}
		if (!vpFormatsSupported.isJsonObject()) {
			throw error("vp_formats_supported must be a JSON object",
				args("vp_formats_supported", vpFormatsSupported));
		}

		JsonObject formats = vpFormatsSupported.getAsJsonObject();
		String credentialFormat = env.getString("credential_format");

		switch (credentialFormat) {
			case "iso_mdl" -> validateMsoMdoc(formats);
			case "sd_jwt_vc" -> validateSdJwtVc(formats);
			default -> throw error("Unknown credential_format", args("credential_format", credentialFormat));
		}

		return env;
	}

	private void validateMsoMdoc(JsonObject formats) {
		JsonElement msoMdoc = formats.get("mso_mdoc");
		if (msoMdoc == null) {
			throw error("vp_formats_supported does not contain 'mso_mdoc' but the test is configured for ISO mDL credential format",
				args("vp_formats_supported", formats));
		}
		if (!msoMdoc.isJsonObject()) {
			throw error("vp_formats_supported.mso_mdoc must be a JSON object",
				args("mso_mdoc", msoMdoc));
		}
		JsonElement alg = msoMdoc.getAsJsonObject().get("alg");
		if (alg == null || !alg.isJsonArray() || alg.getAsJsonArray().isEmpty()) {
			throw error("vp_formats_supported.mso_mdoc.alg must be a non-empty array of algorithm values",
				args("mso_mdoc", msoMdoc));
		}
		logSuccess("vp_formats_supported contains valid mso_mdoc format with alg values",
			args("mso_mdoc", msoMdoc));
	}

	private void validateSdJwtVc(JsonObject formats) {
		JsonElement dcSdJwt = formats.get("dc+sd-jwt");
		if (dcSdJwt == null) {
			throw error("vp_formats_supported does not contain 'dc+sd-jwt' but the test is configured for SD-JWT VC credential format",
				args("vp_formats_supported", formats));
		}
		if (!dcSdJwt.isJsonObject()) {
			throw error("vp_formats_supported.'dc+sd-jwt' must be a JSON object",
				args("dc+sd-jwt", dcSdJwt));
		}
		JsonObject sdJwtObj = dcSdJwt.getAsJsonObject();

		JsonElement sdJwtAlg = sdJwtObj.get("sd-jwt_alg_values");
		if (sdJwtAlg == null || !sdJwtAlg.isJsonArray() || sdJwtAlg.getAsJsonArray().isEmpty()) {
			throw error("vp_formats_supported.'dc+sd-jwt'.sd-jwt_alg_values must be a non-empty array",
				args("dc+sd-jwt", dcSdJwt));
		}

		JsonElement kbJwtAlg = sdJwtObj.get("kb-jwt_alg_values");
		if (kbJwtAlg == null || !kbJwtAlg.isJsonArray() || kbJwtAlg.getAsJsonArray().isEmpty()) {
			throw error("vp_formats_supported.'dc+sd-jwt'.kb-jwt_alg_values must be a non-empty array",
				args("dc+sd-jwt", dcSdJwt));
		}

		logSuccess("vp_formats_supported contains valid dc+sd-jwt format with algorithm values",
			args("dc+sd-jwt", dcSdJwt));
	}
}

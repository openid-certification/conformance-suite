package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.CoseAlgorithmUtil;
import net.openid.conformance.util.JWSUtil;

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

	// OID4VP 1.0 Final Appendix B.2.2
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
		JsonObject msoMdocObj = msoMdoc.getAsJsonObject();

		validateOptionalCoseAlgArray(msoMdocObj, "issuerauth_alg_values", "mso_mdoc");
		validateOptionalCoseAlgArray(msoMdocObj, "deviceauth_alg_values", "mso_mdoc");

		logSuccess("vp_formats_supported contains valid mso_mdoc format",
			args("mso_mdoc", msoMdoc));
	}

	// OID4VP 1.0 Final Appendix B.3.4
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

		validateOptionalJwsAlgArray(sdJwtObj, "sd-jwt_alg_values", "dc+sd-jwt");
		validateOptionalJwsAlgArray(sdJwtObj, "kb-jwt_alg_values", "dc+sd-jwt");

		logSuccess("vp_formats_supported contains valid dc+sd-jwt format",
			args("dc+sd-jwt", dcSdJwt));
	}

	private void validateOptionalJwsAlgArray(JsonObject parent, String paramName, String formatName) {
		JsonArray array = validateOptionalNonEmptyArray(parent, paramName, formatName);
		if (array == null) {
			return;
		}
		for (JsonElement element : array) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
				throw error(String.format("vp_formats_supported.%s.%s must contain only string values", formatName, paramName),
					args(formatName, parent, "invalid_value", element));
			}
			String alg = OIDFJSON.getString(element);
			if (!JWSUtil.isValidJWSAlgorithm(alg)) {
				throw error(String.format("vp_formats_supported.%s.%s contains unrecognized algorithm '%s'", formatName, paramName, alg),
					args(formatName, parent, "unrecognized_alg", alg, "valid_algorithms", JWSUtil.validJWSAlgorithms()));
			}
		}
	}

	private void validateOptionalCoseAlgArray(JsonObject parent, String paramName, String formatName) {
		JsonArray array = validateOptionalNonEmptyArray(parent, paramName, formatName);
		if (array == null) {
			return;
		}
		for (JsonElement element : array) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
				throw error(String.format("vp_formats_supported.%s.%s must contain COSE algorithm integer identifiers", formatName, paramName),
					args(formatName, parent, "invalid_value", element));
			}
			int coseAlgId = OIDFJSON.getInt(element);
			if (!CoseAlgorithmUtil.isValidCoseSignatureAlgorithm(coseAlgId)) {
				throw error(String.format("vp_formats_supported.%s.%s contains unrecognized COSE algorithm identifier %d", formatName, paramName, coseAlgId),
					args(formatName, parent, "unrecognized_cose_alg", coseAlgId));
			}
		}
	}

	private JsonArray validateOptionalNonEmptyArray(JsonObject parent, String paramName, String formatName) {
		JsonElement value = parent.get(paramName);
		if (value == null) {
			return null;
		}
		if (!value.isJsonArray()) {
			throw error(String.format("vp_formats_supported.%s.%s must be an array", formatName, paramName),
				args(formatName, parent));
		}
		if (value.getAsJsonArray().isEmpty()) {
			throw error(String.format("vp_formats_supported.%s.%s must be a non-empty array when present", formatName, paramName),
				args(formatName, parent));
		}
		return value.getAsJsonArray();
	}
}

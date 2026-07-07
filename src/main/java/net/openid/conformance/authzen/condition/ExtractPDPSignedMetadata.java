package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractPDPSignedMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp"})
	@PostEnvironment(required = {"pdp_signed_metadata"})
	public Environment evaluate(Environment env) {
		JsonElement signedMetadataElem = env.getElementFromObject("pdp", "signed_metadata");
		if (signedMetadataElem == null || signedMetadataElem.isJsonNull()) {
			throw error("Discovery metadata does not contain `signed_metadata`; nothing to extract.");
		}
		if (!signedMetadataElem.isJsonPrimitive() || !signedMetadataElem.getAsJsonPrimitive().isString()) {
			throw error("`signed_metadata` must be a JWT string", args("signed_metadata", signedMetadataElem));
		}
		JsonObject signedMetadata = parseJwt("signed_metadata", OIDFJSON.getString(signedMetadataElem));
		env.putObject("pdp_signed_metadata", signedMetadata);

		logSuccess("Extracted PDP signed_metadata", args("pdp_signed_metadata", signedMetadata));
		return env;
	}

	protected JsonObject parseJwt(String type, String jwtString) {
		try {
			return JWTUtil.jwtStringToJsonObjectForEnvironment(jwtString);
		} catch (ParseException e) {
			throw error("Couldn't parse jwt from " + type, e, args("jwt", jwtString));
		}
	}
}

package net.openid.conformance.vci10issuer.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIParseCredentialIssuerMetadata extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialIssuerMetadataJson = env.getString("credential_issuer_metadata_endpoint_response", "body");

		if (Strings.isNullOrEmpty(credentialIssuerMetadataJson)) {
			throw error("Missing credential issuer metadata in the response");
		}

		try {
			JsonObject credentialIssuerMetadata = JsonParser.parseString(credentialIssuerMetadataJson).getAsJsonObject();
			env.putObject("vci", "credential_issuer_metadata", credentialIssuerMetadata);

			logSuccess("Successfully parsed credential issuer metadata", credentialIssuerMetadata);
			return env;
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", credentialIssuerMetadataJson));
		}
	}
}

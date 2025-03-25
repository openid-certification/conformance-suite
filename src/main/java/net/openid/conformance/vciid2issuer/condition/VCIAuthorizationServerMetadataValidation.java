package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIAuthorizationServerMetadataValidation extends AbstractSchemaBasedMetadataValidation {

	@Override
	protected MetadataValidationInput createMetadataValidationInput(Environment env) {
		// TODO add support for validating multiple authorization servers
		JsonObject metadata = env.getElementFromObject("vci", "authorization_servers.server0.authorization_server_metadata").getAsJsonObject();
		// we use the oid4vci specific variant of the rfc8414 metadata
		String schemaResource = "json-schemas/oid4vci/rfc8414-oauth_authorization_server_metadata.json";
		String metadataName = "OAuth Authorization Server";
		return new MetadataValidationInput(metadataName, schemaResource, metadata);
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}

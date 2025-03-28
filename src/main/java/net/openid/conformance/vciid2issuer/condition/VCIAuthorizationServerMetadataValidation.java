package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vciid2issuer.util.MetadataValidationInput;

public class VCIAuthorizationServerMetadataValidation extends AbstractSchemaBasedMetadataValidation {

	protected int index;

	public VCIAuthorizationServerMetadataValidation(int index) {
		this.index = index;
	}

	public VCIAuthorizationServerMetadataValidation() {
		this(0);
	}

	public static String getAuthServerMetadataPath(int index) {
		return String.format("authorization_servers.server%d.authorization_server_metadata",index);
	}

	@Override
	protected MetadataValidationInput createMetadataValidationInput(Environment env) {
		String authServerMetadataPath = getAuthServerMetadataPath(0);
		JsonObject metadata = env.getElementFromObject("vci", authServerMetadataPath).getAsJsonObject();
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

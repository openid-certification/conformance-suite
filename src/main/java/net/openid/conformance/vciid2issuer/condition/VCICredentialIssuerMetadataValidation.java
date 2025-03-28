package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vciid2issuer.util.MetadataValidationInput;

public class VCICredentialIssuerMetadataValidation extends AbstractSchemaBasedMetadataValidation {

	@Override
	protected MetadataValidationInput createMetadataValidationInput(Environment env) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		String schemaResource = "json-schemas/oid4vci/credential_issuer_metadata_ID2_15.json";
		String metadataName = "OID4VCI Credential Issuer";
		return new MetadataValidationInput(metadataName, schemaResource, metadata);
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}

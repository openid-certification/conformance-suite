package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

/**
 * Same as {@link AddVP1FinalEncryptionParametersToClientMetadata} but removes the "use" property
 * from the encryption key in client_metadata. This tests that wallets can identify and use the
 * encryption key without an explicit "use": "enc" marker.
 */
public class AddVP1FinalEncryptionParametersToClientMetadataWithoutUseEnc extends AddVP1FinalEncryptionParametersToClientMetadata {

	@Override
	protected JsonObject transformEncKey(JsonObject encKey) {
		JsonObject copy = encKey.deepCopy();
		copy.remove("use");
		return copy;
	}
}

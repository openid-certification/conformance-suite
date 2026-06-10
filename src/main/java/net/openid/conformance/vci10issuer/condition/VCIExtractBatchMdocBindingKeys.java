package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate;
import org.multipaz.mdoc.mso.MobileSecurityObject;

/**
 * Extracts the device key (MSO deviceKeyInfo.deviceKey) from each mdoc credential issued in
 * a batch and stores them, index-aligned with the credentials and converted to JWK form, in
 * 'vci_batch_binding_keys' for the batch key binding checks.
 */
public class VCIExtractBatchMdocBindingKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "extracted_credentials")
	@PostEnvironment(required = "vci_batch_binding_keys")
	public Environment evaluate(Environment env) {

		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");

		JsonArray keys = new JsonArray();
		for (int i = 0; i < list.size(); i++) {
			keys.add(extractDeviceKey(OIDFJSON.getString(list.get(i)), i));
		}

		JsonObject bindingKeys = new JsonObject();
		bindingKeys.add("keys", keys);
		env.putObject("vci_batch_binding_keys", bindingKeys);

		logSuccess("Extracted the MSO device key from each credential in the batch",
			args("binding_keys", keys));

		return env;
	}

	private JsonObject extractDeviceKey(String mdocBase64Url, int index) {
		EcPublicKey deviceKey;
		try {
			byte[] bytes = new Base64URL(mdocBase64Url).decode();
			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);
			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				throw error("mdoc credential is missing the required 'issuerAuth' field",
					args("credential_index", index));
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			DataItem msoDataItem = Cbor.INSTANCE.decode(coseSign1.getPayload()).getAsTaggedEncodedCbor();
			MobileSecurityObject mso = MobileSecurityObject.Companion.fromDataItem(msoDataItem);
			deviceKey = mso.getDeviceKey();
		} catch (ConditionError e) {
			throw e;
		} catch (Exception e) {
			throw error("Failed to extract the device key from the mdoc credential", e,
				args("credential_index", index));
		}

		if (!(deviceKey instanceof EcPublicKeyDoubleCoordinate ecKey)) {
			throw error("The mdoc credential's device key is not an EC key with x/y coordinates",
				args("credential_index", index, "curve", deviceKey.getCurve().name()));
		}

		JsonObject jwk = new JsonObject();
		jwk.addProperty("kty", "EC");
		jwk.addProperty("crv", ecKey.getCurve().getJwkName());
		jwk.addProperty("x", Base64URL.encode(ecKey.getX()).toString());
		jwk.addProperty("y", Base64URL.encode(ecKey.getY()).toString());
		return jwk;
	}
}

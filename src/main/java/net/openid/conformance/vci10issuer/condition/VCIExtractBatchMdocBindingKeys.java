package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.MdocUtil;

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
		byte[] bytes = new Base64URL(mdocBase64Url).decode();
		try {
			return MdocUtil.extractDeviceKeyJwk(bytes);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e, args("credential_index", index));
		}
	}
}

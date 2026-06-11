package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OID4VCI 1.0 Final §8.3: "Each key provided by the Wallet is used to bind to, at most,
 * one Credential." (see also §3.3.2: credentials in a batch "SHOULD contain different
 * Cryptographic Data", e.g. to achieve unlinkability) - so no two credentials in the batch
 * may be bound to the same key.
 */
public class VCIEnsureBatchBindingKeysAreDistinct extends AbstractVCIBatchBindingKeyCheck {

	@Override
	@PreEnvironment(required = "vci_batch_binding_keys")
	public Environment evaluate(Environment env) {

		List<String> bindingThumbprints =
			getThumbprints(env, "vci_batch_binding_keys", "credential binding key");

		JsonArray bindingKeys = env.getObject("vci_batch_binding_keys").getAsJsonArray("keys");
		Map<String, Integer> seen = new HashMap<>();
		for (int i = 0; i < bindingThumbprints.size(); i++) {
			Integer firstIndex = seen.putIfAbsent(bindingThumbprints.get(i), i);
			if (firstIndex != null) {
				throw error("Two credentials issued in the batch are bound to the same key; each proof key "
						+ "must be bound to at most one credential",
					args("first_credential_index", firstIndex,
						"second_credential_index", i,
						"binding_key", bindingKeys.get(i)));
			}
		}

		logSuccess("Each credential in the batch is bound to a distinct key");

		return env;
	}
}

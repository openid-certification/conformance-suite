package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OID4VCI 1.0 Final §8.3: the issued credentials are bound to "the number of keys that the
 * Wallet has provided via the proofs parameter of the Credential Request" - so each issued
 * credential's binding key must be one of the proof keys the wallet sent; a credential
 * bound to any other key is an error.
 */
public class VCIEnsureBatchBindingKeysMatchSentProofKeys extends AbstractVCIBatchBindingKeyCheck {

	@Override
	@PreEnvironment(required = {"vci_batch_binding_keys", "vci_batch_proof_public_jwks"})
	public Environment evaluate(Environment env) {

		Set<String> sentThumbprints =
			new HashSet<>(getThumbprints(env, "vci_batch_proof_public_jwks", "sent proof key"));
		List<String> bindingThumbprints =
			getThumbprints(env, "vci_batch_binding_keys", "credential binding key");

		JsonArray bindingKeys = env.getObject("vci_batch_binding_keys").getAsJsonArray("keys");
		for (int i = 0; i < bindingThumbprints.size(); i++) {
			if (!sentThumbprints.contains(bindingThumbprints.get(i))) {
				throw error("A credential issued in the batch is bound to a key that was not one of the keys "
						+ "the proofs in the credential request demonstrated possession of",
					args("credential_index", i, "binding_key", bindingKeys.get(i)));
			}
		}

		logSuccess("Every credential in the batch is bound to one of the proof keys sent in the credential request");

		return env;
	}
}

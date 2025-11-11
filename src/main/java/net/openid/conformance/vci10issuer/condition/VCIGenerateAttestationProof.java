package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCIGenerateAttestationProof extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "key_attestation_jwt")
	@PostEnvironment(required = "credential_request_proofs")
	public Environment evaluate(Environment env) {

		String keyAttestationJwt = env.getString("key_attestation_jwt");

		JsonObject proofsObject = createProofsObject(keyAttestationJwt);
		env.putObject("credential_request_proofs", proofsObject);

		logSuccess("Generated attestation proof object for credential request", args("proofs", proofsObject));

		return env;
	}

	protected JsonObject createProofsObject(String attestationProofJwt) {
		JsonObject proofsObject = new JsonObject();
		proofsObject.add("attestation", OIDFJSON.convertListToJsonArray(List.of(attestationProofJwt)));
		return proofsObject;
	}
}

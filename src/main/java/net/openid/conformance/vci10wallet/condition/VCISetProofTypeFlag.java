package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reads {@code env.proof_type} (set by {@link VCIExtractCredentialRequestProof}) and
 * writes a per-type flag key under {@code env.vci.proof_type_<type>}. Lets downstream
 * proof-type-specific conditions self-gate via {@code skipIfElementMissing("vci",
 * "proof_type_<type>")} so they all live in one ConditionSequence without imperative
 * branching on the proof-type string.
 *
 * <p>Sets exactly one of:
 * <ul>
 *   <li>{@code vci.proof_type_jwt}</li>
 *   <li>{@code vci.proof_type_attestation}</li>
 *   <li>{@code vci.proof_type_di_vp}</li>
 * </ul>
 * with the value {@code "yes"}. If {@code env.proof_type} is missing or any other value,
 * no flag is set (the no-op case for non-cryptographic-binding paths).
 */
public class VCISetProofTypeFlag extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		String proofType = env.getString("proof_type");
		if (proofType == null) {
			log("No proof_type set in environment, no flag written.");
			return env;
		}
		if (!"jwt".equals(proofType) && !"attestation".equals(proofType) && !"di_vp".equals(proofType)) {
			log("Unrecognized proof_type, no flag written.", args("proof_type", proofType));
			return env;
		}
		JsonObject vci = env.getObject("vci");
		vci.addProperty("proof_type_" + proofType, "yes");
		logSuccess("Proof-type flag set.", args("proof_type", proofType, "flag", "vci.proof_type_" + proofType));
		return env;
	}
}

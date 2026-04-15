package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Enforces OID4VCI 1.0 Final §8.2 for the case where the issuer mandates credential response
 * encryption: if {@code credential_response_encryption.encryption_required} is {@code true},
 * then every credential request must include the {@code credential_response_encryption}
 * parameter, and §8.2 in turn requires those requests to be encrypted. The issuer therefore
 * MUST publish {@code credential_request_encryption} metadata so that a wallet can actually
 * comply — otherwise the advertised combination is provably unusable by any conformant wallet.
 *
 * This condition fires only in that strict sub-case. The complementary "footgun" sub-case
 * (response encryption declared with {@code encryption_required=false} and
 * {@code credential_request_encryption} absent) is handled by
 * {@link VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionOptional}.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionRequired extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonElement responseEncryptionEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_response_encryption");
		if (responseEncryptionEl == null) {
			logSuccess("credential_response_encryption is not declared");
			return env;
		}

		JsonElement encryptionRequiredEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_response_encryption.encryption_required");
		boolean responseEncryptionRequired = encryptionRequiredEl != null
			&& encryptionRequiredEl.isJsonPrimitive()
			&& encryptionRequiredEl.getAsJsonPrimitive().isBoolean()
			&& OIDFJSON.getBoolean(encryptionRequiredEl);

		if (!responseEncryptionRequired) {
			logSuccess("credential_response_encryption.encryption_required is false or absent");
			return env;
		}

		boolean requestEncryptionDeclared = env.getElementFromObject("vci", "credential_issuer_metadata.credential_request_encryption") != null;
		if (!requestEncryptionDeclared) {
			throw error("credential_response_encryption.encryption_required is true but credential_request_encryption is missing.");
		}

		logSuccess("credential_response_encryption.encryption_required is true and credential_request_encryption is declared");
		return env;
	}
}

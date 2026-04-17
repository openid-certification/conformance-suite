package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Flags the "footgun" sub-case of OID4VCI 1.0 Final §8.2: the issuer declares
 * {@code credential_response_encryption} with {@code encryption_required=false} but does not
 * publish {@code credential_request_encryption}. Wallets that never opt into response
 * encryption still interoperate, so the metadata is not strictly unusable — but any wallet
 * that does opt into response encryption will be unable to satisfy §8.2 ("Credential Request
 * encryption MUST be used if the credential_response_encryption parameter is included, to
 * prevent it being substituted by an attacker") because there is no request encryption key
 * to encrypt to.
 *
 * Callers pick the severity. In the generic metadata test the partial metadata is a WARNING
 * (footgun); in the encrypted-variant runtime flow the caller invokes this at FAILURE because
 * the variant is exactly the opt-in case that the footgun breaks.
 *
 * The complementary strict sub-case (response encryption mandatory, request encryption absent)
 * is handled by {@link VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionRequired}.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4">OID4VCI Section 12.2.4 - Credential Issuer Metadata</a>
 */
public class VCIEnsureCredentialRequestEncryptionWhenResponseEncryptionOptional extends AbstractCondition {

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

		if (responseEncryptionRequired) {
			logSuccess("credential_response_encryption.encryption_required is true.");
			return env;
		}

		boolean requestEncryptionDeclared = env.getElementFromObject("vci", "credential_issuer_metadata.credential_request_encryption") != null;
		if (!requestEncryptionDeclared) {
			throw error("credential_response_encryption is declared (with encryption_required=false) but credential_request_encryption is missing.");
		}

		logSuccess("credential_response_encryption is declared (with encryption_required=false) and credential_request_encryption is present");
		return env;
	}
}

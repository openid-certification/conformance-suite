package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the helper that decides whether the encrypted variant of the wallet test should be
 * skipped because the wallet did not (and is not required to) use credential request/response
 * encryption. The skip decision is the regression target for the case where a wallet without
 * encryption support was incorrectly failed with a misleading "Section 8.2" error.
 *
 * <p>The helpers under test are protected instance methods on {@link AbstractVCIWalletTest} —
 * subclasses may override them. The tests instantiate the concrete
 * {@link VCIWalletTestCredentialIssuance} and seed {@code env} via an instance initializer
 * (the only way to reach the inherited protected field without a dedicated test subclass).
 */
public class AbstractVCIWalletTest_UnitTest {

	@Test
	public void skipsWhenWalletSendsPlaintextAndIssuerDoesNotRequireEncryption() {
		AbstractVCIWalletTest t = newTest("application/json", "{\"credential_identifier\":\"X\"}", false);
		assertTrue(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void doesNotSkipWhenWalletEncryptedTheRequest() {
		AbstractVCIWalletTest t = newTest("application/jwt", "eyJ.dummy.jwe", false);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void doesNotSkipWhenIssuerRequiresRequestEncryption() {
		AbstractVCIWalletTest t = newTest("application/json", "{\"credential_identifier\":\"X\"}", true);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void doesNotSkipWhenWalletAskedForResponseEncryption() {
		AbstractVCIWalletTest t = newTest("application/json",
			"{\"credential_identifier\":\"X\",\"credential_response_encryption\":{\"jwk\":{},\"enc\":\"A256GCM\"}}",
			false);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void treatsApplicationJwtContentTypeWithCharsetParameterAsEncrypted() {
		AbstractVCIWalletTest t = newTest("application/jwt; charset=utf-8", "eyJ.dummy.jwe", false);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void doesNotSkipWhenContentTypeIsMissing() {
		// A plaintext credential request MUST set Content-Type to application/json per
		// Section 8.2-11. A missing Content-Type is a spec violation in its own right and must
		// not be silently skipped — the downstream decrypt condition fails it.
		AbstractVCIWalletTest t = newTest(null, "{\"credential_identifier\":\"X\"}", false);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void doesNotSkipWhenContentTypeIsUnrecognized() {
		// Any Content-Type other than application/json (plaintext) or application/jwt
		// (encrypted) should fall through to the decrypt condition, not silently skip.
		AbstractVCIWalletTest t = newTest("application/jwt+json", "irrelevant", false);
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	@Test
	public void subclassCanOverrideSkipDecision() {
		// Subclasses may override the skip decision. Verify the override hook is invoked.
		AbstractVCIWalletTest t = new VCIWalletTestCredentialIssuance() {
			{
				populateIncomingRequest(env, "application/json", "{\"credential_identifier\":\"X\"}");
				populateIssuerMetadata(env, false);
			}

			@Override
			protected boolean shouldSkipDueToWalletNotUsingEncryption() {
				return false;
			}
		};
		assertFalse(t.shouldSkipDueToWalletNotUsingEncryption());
	}

	private static AbstractVCIWalletTest newTest(String contentType, String body, boolean issuerRequiresRequestEncryption) {
		return new VCIWalletTestCredentialIssuance() {
			{
				populateIncomingRequest(env, contentType, body);
				populateIssuerMetadata(env, issuerRequiresRequestEncryption);
			}
		};
	}

	private static void populateIncomingRequest(net.openid.conformance.testmodule.Environment env, String contentType, String body) {
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("headers", headers);
		incomingRequest.addProperty("body", body);
		try {
			incomingRequest.add("body_json", JsonParser.parseString(body));
		} catch (com.google.gson.JsonSyntaxException ignored) {
			// body is not JSON (e.g. a JWE compact serialization) — leave body_json absent
		}
		env.putObject("incoming_request", incomingRequest);
	}

	private static void populateIssuerMetadata(net.openid.conformance.testmodule.Environment env, boolean issuerRequiresRequestEncryption) {
		JsonObject requestEnc = new JsonObject();
		requestEnc.addProperty("encryption_required", issuerRequiresRequestEncryption);
		JsonObject metadata = new JsonObject();
		metadata.add("credential_request_encryption", requestEnc);
		env.putObject("credential_issuer_metadata", metadata);
	}
}

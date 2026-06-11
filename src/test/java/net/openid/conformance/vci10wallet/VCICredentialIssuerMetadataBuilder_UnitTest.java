package net.openid.conformance.vci10wallet;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VCICredentialIssuerMetadataBuilder_UnitTest {

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putString("base_url", "https://issuer.example.com/test/a/abc");
		env.putString("base_mtls_url", "https://issuer-mtls.example.com/test/a/abc");
	}

	private VCICredentialIssuerMetadataBuilder.Config config(Integer batchSize) {
		return new VCICredentialIssuerMetadataBuilder.Config(
			"credential",
			"nonce",
			"deferred",
			"notification",
			false,
			true,
			true,
			false,
			batchSize);
	}

	@Test
	public void testBuild_advertisesBatchCredentialIssuanceWhenBatchSizeSet() {
		JsonObject metadata = VCICredentialIssuerMetadataBuilder.buildCredentialIssuerMetadata(env, config(10));

		assertTrue(metadata.has("batch_credential_issuance"));
		assertEquals(10, OIDFJSON.getInt(
			metadata.getAsJsonObject("batch_credential_issuance").get("batch_size")));
	}

	@Test
	public void testBuild_omitsBatchCredentialIssuanceWhenBatchSizeNull() {
		JsonObject metadata = VCICredentialIssuerMetadataBuilder.buildCredentialIssuerMetadata(env, config(null));

		assertFalse(metadata.has("batch_credential_issuance"));
	}
}

package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ValidatePDPSignedMetadataIss_UnitTest {

	private static final String PDP_ISSUER = "https://pdp.example.com";

	private static final String THIRD_PARTY_ISSUER = "https://other.example.com";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPSignedMetadataIss cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPSignedMetadataIss();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaimsIss(String iss) {
		JsonObject claims = new JsonObject();
		if (iss != null) {
			claims.addProperty("iss", iss);
		}
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", claims);
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	private void putConfig(String policyDecisionPoint, String metadataIssuer) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (policyDecisionPoint != null) {
			pdp.addProperty("policy_decision_point", policyDecisionPoint);
		}
		if (metadataIssuer != null) {
			pdp.addProperty("metadata_issuer", metadataIssuer);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	private void putExpectedIssuer(String value) {
		putConfig(value, null);
	}

	@Test
	public void matchingIssuer_succeeds() {
		putClaimsIss(PDP_ISSUER);
		putExpectedIssuer(PDP_ISSUER);
		cond.execute(env);
	}

	@Test
	public void missingIssuerClaim_fails() {
		putClaimsIss(null);
		putExpectedIssuer(PDP_ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyIssuerClaim_fails() {
		putClaimsIss("");
		putExpectedIssuer(PDP_ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void issuerMismatch_fails() {
		// With no 'Signed Metadata Issuer' configured the metadata must be self-attested by the PDP.
		putClaimsIss(THIRD_PARTY_ISSUER);
		putExpectedIssuer(PDP_ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void configuredMetadataIssuerMatches_succeeds() {
		// AuthZEN §11.8 allows a third party to attest to the signed metadata; declaring it in the
		// 'Signed Metadata Issuer' field is how the tester marks that issuer as the expected one.
		putClaimsIss(THIRD_PARTY_ISSUER);
		putConfig(PDP_ISSUER, THIRD_PARTY_ISSUER);
		cond.execute(env);
	}

	@Test
	public void configuredMetadataIssuerTakesPrecedenceOverPdpIdentifier() {
		// Once set, the configured issuer is authoritative: an `iss` equal to the PDP Identifier but
		// not to the configured value is a mismatch.
		putClaimsIss(PDP_ISSUER);
		putConfig(PDP_ISSUER, THIRD_PARTY_ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void blankMetadataIssuerFallsBackToPdpIdentifier() {
		// An empty string must behave exactly like an absent field.
		putClaimsIss(PDP_ISSUER);
		putConfig(PDP_ISSUER, "");
		cond.execute(env);
	}

	@Test
	public void missingExpectedIssuerInConfig_fails() {
		putClaimsIss(PDP_ISSUER);
		putConfig(null, null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingPdpIdentifierButMetadataIssuerConfigured_succeeds() {
		putClaimsIss(THIRD_PARTY_ISSUER);
		putConfig(null, THIRD_PARTY_ISSUER);
		cond.execute(env);
	}
}

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

	private void putExpectedIssuer(String value) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (value != null) {
			pdp.addProperty("policy_decision_point", value);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
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
	public void issuerMismatch_fails() {
		putClaimsIss("https://other.example.com");
		putExpectedIssuer(PDP_ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingExpectedIssuerInConfig_fails() {
		putClaimsIss(PDP_ISSUER);
		putExpectedIssuer(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

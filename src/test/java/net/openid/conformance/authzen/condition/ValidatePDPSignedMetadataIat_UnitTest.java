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
class ValidatePDPSignedMetadataIat_UnitTest {

	private static final long NOW = System.currentTimeMillis() / 1000L;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPSignedMetadataIat cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPSignedMetadataIat();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaims(JsonObject claims) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", claims);
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	private void putIat(long iat) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", iat);
		putClaims(claims);
	}

	@Test
	public void noIat_skips() {
		// iat is optional; absence is logged and accepted.
		putClaims(new JsonObject());
		cond.execute(env);
	}

	@Test
	public void recentIat_succeeds() {
		putIat(NOW - 60);
		cond.execute(env);
	}

	@Test
	public void futureIat_succeeds() {
		// RFC 7519 gives iat no validity rule; CheckPDPSignedMetadataTimeClaimsArePlausible warns about it.
		putIat(NOW + 3600);
		cond.execute(env);
	}

	@Test
	public void oldIat_succeeds() {
		// RFC 7519 sets no lower bound; CheckPDPSignedMetadataTimeClaimsArePlausible warns about it.
		putIat(1_000_000_000L);
		cond.execute(env);
	}
}

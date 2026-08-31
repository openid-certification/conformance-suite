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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The caller runs this only when the PDP's discovery metadata carried no `signed_metadata`, and
 * invokes it with ConditionResult.WARNING — signed metadata is OPTIONAL, so a configured issuer with
 * nothing to attest to is the tester's problem, not a PDP conformance failure.
 */
@ExtendWith(MockitoExtension.class)
class WarnUnusedSignedMetadataIssuer_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnUnusedSignedMetadataIssuer cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnUnusedSignedMetadataIssuer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void putMetadataIssuer(String value) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (value != null) {
			pdp.addProperty("metadata_issuer", value);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	@Test
	public void notConfigured_succeeds() {
		putMetadataIssuer(null);
		cond.execute(env);
	}

	@Test
	public void configuredButEmpty_succeeds() {
		putMetadataIssuer("");
		cond.execute(env);
	}

	@Test
	public void configuredWithWrongType_warns() {
		// Nothing has validated the field's type on this path — ValidatePDPMetadataIssuer only runs when
		// signed_metadata is present — so reading it as a string here would throw UnexpectedTypeException
		// and fail the test with an internal error instead of the intended warning.
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		pdp.addProperty("metadata_issuer", 42);
		config.add("pdp", pdp);
		env.putObject("config", config);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no `signed_metadata` for it to apply to"), e.getMessage());
	}

	@Test
	public void configuredWithNoSignedMetadata_warns() {
		putMetadataIssuer("https://attester.example.com");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no `signed_metadata` for it to apply to"), e.getMessage());
	}
}

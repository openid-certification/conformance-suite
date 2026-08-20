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

@ExtendWith(MockitoExtension.class)
class ValidatePDPMetadataIssuer_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPMetadataIssuer cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPMetadataIssuer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
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
		// The field is optional: without it the `iss` claim is checked against the PDP Identifier.
		putMetadataIssuer(null);
		cond.execute(env);
	}

	@Test
	public void empty_succeeds() {
		// An empty string must behave exactly like an absent field, as ValidatePDPSignedMetadataIss treats it.
		putMetadataIssuer("");
		cond.execute(env);
	}

	@Test
	public void httpsUrl_succeeds() {
		putMetadataIssuer("https://attester.example.com");
		cond.execute(env);
	}

	@Test
	public void urlWithPath_succeeds() {
		putMetadataIssuer("https://example.com/tenants/1");
		cond.execute(env);
	}

	@Test
	public void urn_succeeds() {
		// RFC 7519 requires a colon-bearing value to be a URI, not specifically an https URL: an
		// attesting party is under no obligation to identify itself by one.
		putMetadataIssuer("urn:example:metadata-attester");
		cond.execute(env);
	}

	@Test
	public void bareStringWithoutColon_succeeds() {
		// StringOrURI allows arbitrary strings as long as they carry no colon.
		putMetadataIssuer("metadata-attester");
		cond.execute(env);
	}

	@Test
	public void surroundingWhitespace_fails() {
		// Compared verbatim against `iss`, so this could only ever produce a confusing mismatch.
		putMetadataIssuer(" https://attester.example.com ");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("whitespace"), e.getMessage());
	}

	@Test
	public void trailingNewline_fails() {
		putMetadataIssuer("https://attester.example.com\n");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void colonBearingValueThatIsNotAUri_fails() {
		putMetadataIssuer("https://attester example.com");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("URI"), e.getMessage());
	}

	@Test
	public void relativeUriReference_fails() {
		putMetadataIssuer("//attester.example.com:8443/x");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void notAString_fails() {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		pdp.addProperty("metadata_issuer", 42);
		config.add("pdp", pdp);
		env.putObject("config", config);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("must be a string"), e.getMessage());
	}
}

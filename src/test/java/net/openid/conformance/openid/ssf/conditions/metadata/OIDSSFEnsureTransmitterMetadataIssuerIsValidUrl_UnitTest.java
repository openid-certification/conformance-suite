package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureTransmitterMetadataIssuerIsValidUrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureTransmitterMetadataIssuerIsValidUrl createCondition() {
		OIDSSFEnsureTransmitterMetadataIssuerIsValidUrl condition = new OIDSSFEnsureTransmitterMetadataIssuerIsValidUrl();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepareIssuer(String issuer) {
		JsonObject transmitterMetadata = new JsonObject();
		if (issuer != null) {
			transmitterMetadata.addProperty("issuer", issuer);
		}
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", transmitterMetadata);
		env.putObject("ssf", ssf);
	}

	@Test
	void passesForHttpsUrlWithPath() {
		prepareIssuer("https://transmitter.example.com/ssf/tenant-1");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesForHttpsUrlWithPort() {
		prepareIssuer("https://localhost.emobix.co.uk:8443/test/a/ssf");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForHttpScheme() {
		prepareIssuer("http://transmitter.example.com/");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForQueryComponent() {
		prepareIssuer("https://transmitter.example.com/ssf?tenant=1");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForFragmentComponent() {
		prepareIssuer("https://transmitter.example.com/ssf#frag");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesForMissingIssuerAsThatIsGradedSeparately() {
		prepareIssuer(null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForNonUrl() {
		prepareIssuer("not a url");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsForNonStringIssuer() {
		JsonObject transmitterMetadata = new JsonObject();
		transmitterMetadata.addProperty("issuer", 42);
		JsonObject ssf = new JsonObject();
		ssf.add("transmitter_metadata", transmitterMetadata);
		env.putObject("ssf", ssf);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}
}

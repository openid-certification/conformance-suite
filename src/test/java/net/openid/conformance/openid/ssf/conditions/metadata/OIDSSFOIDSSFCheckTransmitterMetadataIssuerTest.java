package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFOIDSSFCheckTransmitterMetadataIssuerTest {

	OIDSSFCheckTransmitterMetadataIssuer issuerCheck;

	@Mock
	TestInstanceEventLog eventLog;

	Environment env;

	@BeforeEach
	public void setUp() {
		issuerCheck = new OIDSSFCheckTransmitterMetadataIssuer();
		issuerCheck.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env = new Environment();
	}

	@Test
	public void shouldPassValidatingPlainCorrectIssuer() {
		env.putString("config", "ssf.transmitter.issuer", "https://issuer.com");

		JsonObject ssfTransmitterMetadata = new JsonObject();
		ssfTransmitterMetadata.addProperty("issuer", "https://issuer.com");
		env.putObject("ssf", "transmitter_metadata", ssfTransmitterMetadata);

		issuerCheck.evaluate(env);
	}

	@Test
	public void shouldFailValidatingPlainIncorrectIssuer() {
		env.putString("config", "ssf.transmitter.issuer", "https://issuer.com");

		JsonObject ssfTransmitterMetadata = new JsonObject();
		ssfTransmitterMetadata.addProperty("issuer", "https://example.com");
		env.putObject("ssf", "transmitter_metadata", ssfTransmitterMetadata);

		ConditionError ex = assertThrows(ConditionError.class, () -> issuerCheck.evaluate(env));

		assertEquals("OIDSSFCheckTransmitterMetadataIssuer: issuer listed in the SSF Configuration document is not consistent with the location the discovery document was retrieved from and the provided metadata suffix. These must match to prevent impersonation attacks.", ex.getMessage());
	}

	@Test
	public void shouldPassValidatingPlainCorrectIssuerWithMetadataSuffix() {
		env.putString("config", "ssf.transmitter.issuer", "https://issuer.com");
		env.putString("config", "ssf.transmitter.metadata_suffix", "/tenants/1234");

		JsonObject ssfTransmitterMetadata = new JsonObject();
		ssfTransmitterMetadata.addProperty("issuer", "https://issuer.com/tenants/1234");
		env.putObject("ssf", "transmitter_metadata", ssfTransmitterMetadata);

		issuerCheck.evaluate(env);
	}

	@Test
	public void shouldFailValidatingPlainIncorrectIssuerWithMetadataSuffix() {
		env.putString("config", "ssf.transmitter.issuer", "https://issuer.com");
		env.putString("config", "ssf.transmitter.metadata_suffix", "/tenants/invalid");

		JsonObject ssfTransmitterMetadata = new JsonObject();
		ssfTransmitterMetadata.addProperty("issuer", "https://issuer.com/tenants/1234");
		env.putObject("ssf", "transmitter_metadata", ssfTransmitterMetadata);

		ConditionError ex = assertThrows(ConditionError.class, () -> issuerCheck.evaluate(env));

		assertEquals("OIDSSFCheckTransmitterMetadataIssuer: issuer listed in the SSF Configuration document is not consistent with the location the discovery document was retrieved from and the provided metadata suffix. These must match to prevent impersonation attacks.", ex.getMessage());
	}
}

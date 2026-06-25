package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
class EnsurePDPSignedMetadataDoesNotContainSignedMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePDPSignedMetadataDoesNotContainSignedMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePDPSignedMetadataDoesNotContainSignedMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClaims(String claimsJson) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("claims", JsonParser.parseString(claimsJson).getAsJsonObject());
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	@Test
	public void noNestedSignedMetadata_succeeds() {
		putClaims("{ \"iss\": \"https://pdp.example.com\", \"access_evaluation_endpoint\": \"https://pdp.example.com/eval\" }");
		cond.execute(env);
	}

	@Test
	public void nestedSignedMetadata_fails() {
		putClaims("{ \"iss\": \"https://pdp.example.com\", \"signed_metadata\": \"ey...\" }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

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
class ValidatePDPSignedMetadataAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePDPSignedMetadataAlg cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePDPSignedMetadataAlg();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putHeader(String headerJson) {
		JsonObject signedMetadata = new JsonObject();
		signedMetadata.add("header", JsonParser.parseString(headerJson).getAsJsonObject());
		env.putObject("pdp_signed_metadata", signedMetadata);
	}

	@Test
	public void validAlg_succeeds() {
		putHeader("{ \"alg\": \"RS256\" }");
		cond.execute(env);
	}

	@Test
	public void algNone_fails() {
		putHeader("{ \"alg\": \"none\" }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void algNoneAnyCase_fails() {
		putHeader("{ \"alg\": \"NONE\" }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void algMissing_fails() {
		putHeader("{ \"typ\": \"JWT\" }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}

package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env = new Environment();
	}

	private void putAccept(String accept) {
		JsonObject headers = new JsonObject();
		if (accept != null) {
			headers.addProperty("accept", accept);
		}
		JsonObject request = new JsonObject();
		request.add("headers", headers);
		env.putObject("incoming_request", request);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/jwt",
		"application/jwt, application/json",
		"application/jwt,application/json;q=0.5",
		"application/json; charset=utf-8",
		"application/jwt;q=1, */*;q=0.1",
		"text/plain; foo=\"a, b;c\"",
	})
	public void testEvaluate_validAcceptHeaders(String accept) {
		putAccept(accept);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/jwt; application/json",
		"application/jwt,, application/json",
		"application/jwt, ",
		"application",
		"application/jwt; q = 0.5",
	})
	public void testEvaluate_invalidAcceptHeaders(String accept) {
		putAccept(accept);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_absentAcceptHeader() {
		putAccept(null);
		assertDoesNotThrow(() -> cond.execute(env));
	}
}

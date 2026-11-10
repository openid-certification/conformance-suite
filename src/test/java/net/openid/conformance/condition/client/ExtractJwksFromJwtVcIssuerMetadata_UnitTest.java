package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractJwksFromJwtVcIssuerMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractJwksFromJwtVcIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractJwksFromJwtVcIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_extractsInlineJwks() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\",\"jwks\":{\"keys\":[]}}");

		cond.execute(env);

		assertThat(env.getObject("jwt_vc_issuer_metadata_jwks")).isEqualTo(JsonParser.parseString("{\"keys\":[]}"));
		assertThat(env.getString("jwt_vc_issuer_metadata_jwks_uri")).isNull();
	}

	@Test
	public void testEvaluate_extractsJwksUri() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\",\"jwks_uri\":\"https://issuer.example.com/jwks\"}");

		cond.execute(env);

		assertThat(env.getString("jwt_vc_issuer_metadata_jwks_uri")).isEqualTo("https://issuer.example.com/jwks");
		assertFalse(env.containsObject("jwt_vc_issuer_metadata_jwks"));
	}

	@Test
	public void testEvaluate_rejectsBothJwksAndJwksUri() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\",\"jwks\":{\"keys\":[]},\"jwks_uri\":\"https://issuer.example.com/jwks\"}");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("both 'jwks' and 'jwks_uri'");
		assertFalse(env.containsObject("jwt_vc_issuer_metadata_jwks"));
		assertThat(env.getString("jwt_vc_issuer_metadata_jwks_uri")).isNull();
	}

	@Test
	public void testEvaluate_rejectsNeitherJwksNorJwksUri() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\"}");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("neither 'jwks' nor 'jwks_uri'");
	}

	@Test
	public void testEvaluate_rejectsJwksThatIsNotAnObject() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\",\"jwks\":[]}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsJwksUriThatIsNotAString() {
		putMetadata("{\"issuer\":\"https://issuer.example.com\",\"jwks_uri\":42}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putMetadata(String json) {
		env.putObject("jwt_vc_issuer_metadata", JsonParser.parseString(json).getAsJsonObject());
	}
}

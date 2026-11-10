package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
public class ExtractJwtVcIssuerMetadata_UnitTest {

	private static final String ISS = "https://issuer.example.com";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractJwtVcIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractJwtVcIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JsonObject jwtVcIssuer = new JsonObject();
		jwtVcIssuer.addProperty("iss", ISS);
		jwtVcIssuer.addProperty("iss_source", "status list token");
		jwtVcIssuer.addProperty("metadata_url", ISS + "/.well-known/jwt-vc-issuer");
		env.putObject("status_list_token_jwt_vc_issuer", jwtVcIssuer);
	}

	@Test
	public void testEvaluate_extractsMetadataWithMatchingIssuer() {
		putResponse(200, JsonParser.parseString("{\"issuer\":\"" + ISS + "\",\"jwks\":{\"keys\":[]}}"));

		cond.execute(env);

		assertThat(env.getString("jwt_vc_issuer_metadata", "issuer")).isEqualTo(ISS);
	}

	@Test
	public void testEvaluate_rejectsIssuerMismatch() {
		putResponse(200, JsonParser.parseString("{\"issuer\":\"https://somebody-else.example.com\",\"jwks\":{\"keys\":[]}}"));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("not identical to the iss claim");
		assertFalse(env.containsObject("jwt_vc_issuer_metadata"));
	}

	@Test
	public void testEvaluate_rejectsMissingIssuer() {
		putResponse(200, JsonParser.parseString("{\"jwks\":{\"keys\":[]}}"));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("'issuer'");
	}

	@Test
	public void testEvaluate_rejectsBodyThatIsNotAJsonObject() {
		putResponse(200, null);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("not a JSON object");
	}

	@Test
	public void testEvaluate_ignoresResponseThatIsNot200() {
		putResponse(503, JsonParser.parseString("{\"issuer\":\"" + ISS + "\",\"jwks\":{\"keys\":[]}}"));

		cond.execute(env);

		assertFalse(env.containsObject("jwt_vc_issuer_metadata"));
	}

	private void putResponse(int status, JsonElement bodyJson) {
		JsonObject response = new JsonObject();
		response.addProperty("status", status);
		response.addProperty("body", bodyJson == null ? "This is not JSON!" : bodyJson.toString());
		if (bodyJson != null) {
			response.add("body_json", bodyJson);
		}
		env.putObject(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY, response);
	}
}

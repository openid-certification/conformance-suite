package net.openid.conformance.poppy.condition;

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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CreatePoppyClientMetadataDocument_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreatePoppyClientMetadataDocument cond;

	@BeforeEach
	public void setUp() {
		cond = new CreatePoppyClientMetadataDocument();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String baseUrl) {
		env.putObject("client", new JsonObject());
		env.putString("base_url", baseUrl);
		cond.execute(env);
	}

	@Test
	public void producesUrlsFromBaseUrl() {
		run("https://h:8443/test/a/al");
		JsonObject doc = env.getObject("poppy_client_metadata_document");
		assertEquals("https://h:8443/test/a/al/agent.json", OIDFJSONStr(doc, "client_id"));
		assertEquals("https://h:8443/test/a/al/jwks.json", OIDFJSONStr(doc, "jwks_uri"));
		assertEquals("[\"https://h:8443/test/a/al/callback\"]", doc.get("redirect_uris").toString());
		assertEquals("private_key_jwt", OIDFJSONStr(doc, "token_endpoint_auth_method"));
		assertEquals("OpenID Foundation conformance suite", OIDFJSONStr(doc, "client_name"));
		assertEquals("https://h:8443/test/a/al/agent.json", env.getString("client_id"));
		assertEquals("https://h:8443/test/a/al/callback", env.getString("redirect_uri"));
		assertEquals("https://h:8443/test/a/al/agent.json", env.getString("client", "client_id"));
		assertEquals("https://h:8443/test/a/al/jwks.json", env.getString("client", "jwks_uri"));
		assertEquals("[\"https://h:8443/test/a/al/callback\"]", env.getObject("client").get("redirect_uris").toString());
	}

	@Test
	public void trailingSlashIsNormalised() {
		run("https://h:8443/test/a/al/");
		assertEquals("https://h:8443/test/a/al/agent.json", env.getString("client_id"));
	}

	private static String OIDFJSONStr(JsonObject o, String k) {
		return net.openid.conformance.testmodule.OIDFJSON.getString(o.get(k));
	}
}

package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
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

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidatePoppyJsonCoreFields_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidatePoppyJsonCoreFields cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidatePoppyJsonCoreFields();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("poppy_json_final_url", "https://example.com/.well-known/poppy.json");
		env.putString("poppy_domain", "www.example.com");
	}

	private ConditionError failWith(Consumer<JsonObject> mutate) {
		JsonObject json = PoppyTestData.json(PoppyTestData.POPPY_JSON);
		mutate.accept(json);
		env.putObject("poppy_json", json);
		return assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void examplePassesAndDomainIsOverwritten() {
		env.putObject("poppy_json", PoppyTestData.json(PoppyTestData.POPPY_JSON));
		cond.execute(env);
		assertThat(env.getString("poppy_domain")).isEqualTo("example.com");
	}

	@Test
	public void oldVersionFails() {
		ConditionError e = failWith(j -> j.addProperty("protocol_version", "0.3"));
		assertThat(e.getMessage()).contains("0.4");
	}

	@Test
	public void differentDomainFails() {
		failWith(j -> j.getAsJsonObject("organization").addProperty("domain", "other.example"));
	}

	@Test
	public void domainWithPortFails() {
		failWith(j -> j.getAsJsonObject("organization").addProperty("domain", "example.com:443"));
	}

	@Test
	public void noAgentApisOrWebFails() {
		failWith(j -> {
			j.remove("agent");
			j.remove("apis");
			j.remove("web");
		});
	}

	@Test
	public void httpBrowserSessionEndpointFails() {
		failWith(j -> j.getAsJsonObject("web").addProperty("browser_session_endpoint", "http://example.com/x"));
	}

	@Test
	public void httpApiUrlFails() {
		failWith(j -> j.getAsJsonArray("apis").get(0).getAsJsonObject().addProperty("url", "http://api.example.com/openapi.json"));
	}

	@Test
	public void httpAgentEndpointFails() {
		failWith(j -> j.getAsJsonObject("agent").getAsJsonArray("protocols").get(0).getAsJsonObject().addProperty("endpoint", "http://api.example.com/c"));
	}

	@Test
	public void httpFeedbackEndpointFails() {
		failWith(j -> j.getAsJsonObject("feedback").addProperty("endpoint", "http://api.example.com/f"));
	}

	@Test
	public void httpExcludedTasksUrlFails() {
		failWith(j -> j.getAsJsonObject("excluded_tasks").addProperty("url", "http://example.com/e.json"));
	}

	@Test
	public void httpLogoFails() {
		failWith(j -> j.getAsJsonObject("organization").addProperty("logo_uri", "http://example.com/logo.png"));
	}

	@Test
	public void httpAuthIssuerFails() {
		failWith(j -> j.getAsJsonObject("auth").addProperty("issuer", "http://auth.example.com"));
	}

	@Test
	public void missingOrganizationNameFails() {
		ConditionError e = failWith(j -> j.getAsJsonObject("organization").remove("name"));
		assertThat(e.getMessage()).contains("organization.name");
	}

	@Test
	public void emptyOrganizationNameFails() {
		ConditionError e = failWith(j -> j.getAsJsonObject("organization").addProperty("name", ""));
		assertThat(e.getMessage()).contains("organization.name");
	}
}

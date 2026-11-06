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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExtractPoppyCompanyDomainFromConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractPoppyCompanyDomainFromConfig cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractPoppyCompanyDomainFromConfig();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void configure(String domain) {
		JsonObject poppy = new JsonObject();
		if (domain != null) {
			poppy.addProperty("company_domain", domain);
		}
		JsonObject config = new JsonObject();
		config.add("poppy", poppy);
		env.putObject("config", config);
	}

	@Test
	public void urlWithTrailingSlashAndMixedCase() {
		configure("https://Example.com/");
		cond.execute(env);
		assertThat(env.getString("poppy_domain")).isEqualTo("example.com");
	}

	@Test
	public void bareHostMixedCase() {
		configure("Example.com");
		cond.execute(env);
		assertThat(env.getString("poppy_domain")).isEqualTo("example.com");
	}

	@Test
	public void bareHostWithPortRejected() {
		configure("example.com:8443");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("default HTTPS port");
	}

	@Test
	public void urlWithPortRejected() {
		configure("https://example.com:8443");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("default HTTPS port");
	}

	@Test
	public void httpUrlRejected() {
		configure("http://example.com");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void urlWithPathRejected() {
		configure("https://example.com/foo");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingFieldRejected() {
		configure(null);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("'Company domain' field is missing from the 'Poppy' section in the test configuration");
	}
}

package net.openid.conformance.openid.ssf.conditions.metadata;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class OIDSSFGetDynamicTransmitterConfiguration_UnitTest {

	private OIDSSFGetDynamicTransmitterConfiguration cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new OIDSSFGetDynamicTransmitterConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void stripsTrailingSlashWhenBuildingSsfConfigurationUrl() {
		// OpenID SSF 1.0 sections 7.2 and 7.2.1 require removing any terminating
		// "/" before inserting "/.well-known/ssf-configuration".
		// https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-7.2.1
		env.putString("config", "ssf.transmitter.issuer", "https://tr.example.com/issuer1/");

		String metadataEndpointUrl = cond.buildMetadataEndpointUrl(env);

		assertEquals("https://tr.example.com/.well-known/ssf-configuration/issuer1", metadataEndpointUrl);
	}

	@Test
	public void buildsSsfConfigurationUrlWithoutPath() {
		env.putString("config", "ssf.transmitter.issuer", "https://tr.example.com/");

		String metadataEndpointUrl = cond.buildMetadataEndpointUrl(env);

		assertEquals("https://tr.example.com/.well-known/ssf-configuration", metadataEndpointUrl);
	}

	@Test
	public void appendsConfiguredMetadataSuffixAfterSsfConfigurationUrl() {
		env.putString("config", "ssf.transmitter.issuer", "https://tr.example.com/issuer1/");
		env.putString("config", "ssf.transmitter.metadata_suffix", "/tenants/1234");

		String metadataEndpointUrl = cond.buildMetadataEndpointUrl(env);

		assertEquals("https://tr.example.com/.well-known/ssf-configuration/issuer1/tenants/1234", metadataEndpointUrl);
	}
}

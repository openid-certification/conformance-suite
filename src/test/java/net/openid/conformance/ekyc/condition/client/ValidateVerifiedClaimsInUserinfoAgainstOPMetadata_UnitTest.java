package net.openid.conformance.ekyc.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
public class ValidateVerifiedClaimsInUserinfoAgainstOPMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsInUserinfoAgainstOPMetadata cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsInUserinfoAgainstOPMetadata();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTest(String verifiedClaimsFilename, String opMetadataFilename) throws IOException {
		String verifiedClaimsJson = IOUtils.resourceToString(verifiedClaimsFilename, StandardCharsets.UTF_8, getClass().getClassLoader());
		String opMetadataJson = IOUtils.resourceToString(opMetadataFilename, StandardCharsets.UTF_8, getClass().getClassLoader());

		env.putObjectFromJsonString("verified_claims_response", "userinfo", verifiedClaimsJson);
		env.putObjectFromJsonString("server", opMetadataJson);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() throws Exception {
		String verifiedClaims = "ValidateVerifiedClaimsInUserinfoAgainstOPMetadata/verified-claims.json";
		String opMetadata = "ValidateVerifiedClaimsInUserinfoAgainstOPMetadata/op-metadata.json";

		runTest(verifiedClaims, opMetadata);
	}
}

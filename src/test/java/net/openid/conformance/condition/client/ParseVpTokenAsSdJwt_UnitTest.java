package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ParseVpTokenAsSdJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ParseVpTokenAsSdJwt cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ParseVpTokenAsSdJwt();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		String sdJwtStr = "eyJjdHkiOiJjcmVkZW50aWFsLWNsYWltcy1zZXQranNvbiIsInR5cCI6InZjK3NkLWp3dCIsImFsZyI6IkVTMjU2In0."+
			"eyJjcmVkZW50aWFsU3ViamVjdCI6eyJfc2QiOlsiQXN0c3NCb0tVbVNiREZXdUZmMmtRNU5CbFMtR1NBejF2LVhmQkJSQnJESSIsImw4VTl"+
			"pcUphUzFlWmxheDRCdEF3WTJNaS1aMUxXSTdHa1dBYnBtVndCaWMiLCJ5QkYtMUpIclJJczJBdVVIdEd5WktuQ3RNaUNtNzRxZEpreU5XVUdS" +
			"NDcwIl19LCJfc2RfYWxnIjoic2hhLTI1NiIsImlzcyI6ImRpZDpqd2s6ZXlKcmRIa2lPaUpGUXlJc0ltTnlkaUk2SWxBdE1qVTJJaXdpZUN" +
			"JNkluQlJNWFpZZEZOVmRGRmxXWEY2U2t4aUxXVXlaV000TTJKR2RFazJkbXg2VG5SbE4yUXdPRkZLZWpRaUxDSjVJam9pVW5abU9WbFVSME" +
			"ZyTVRCNk56SnpSbTB6UldsbVZqaFNSVTl1WWs1eFgxWlhZMlZoT0d4NFVtZzBUU0lzSW1Gc1p5STZJa1ZUTWpVMkluMD0iLCJjbmYiOnsian" +
			"drIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiQVNDVW1OQ2dQTk9BVFJiZDhrc3UxdVVNQmpkLXYzVElYNjNxSEtsQzZVQSIsIn" +
			"kiOiJWc055Y1Rkb3ZZb1p2bHVtbTJPTjFQc0tqelFGald1cmNZYjFWS2o1TzFzIn19LCJ0eXBlIjoiVmVyaWZpZWRFTWFpbCIsImV4cCI6M" +
			"TY4MzQ3MDc3NywiaWF0IjoxNjgyNjA2Nzc3fQ.-1lAonblykatcmb7tmJYmI4SmsRSWLp1TmujK0nlvgqYuw-bP2Me29fBnnvQrmh-phW6i" +
			"K7XG1LbQoe7fbhlUQ~WyJsUWlWQVBub1V0Vlo5Z3NHVGhobGlBIiwiZW1haWwiLCJqb3NlcGhAaGVlbmFuLm1lLnVrIl0~eyJ0eXAiOiJKV1" +
			"QiLCJhbGciOiJFUzI1NiJ9.eyJub25jZSI6IjV1Mm1sMmJFTUUiLCJpYXQiOjE2OTA5MDQ5OTUsImF1ZCI6Imh0dHBzOi8vbG9jYWxob3N0Lm" +
			"Vtb2JpeC5jby51azo4NDQzL3Rlc3QvYS9vaWRmLXZjLXRlc3QvY2FsbGJhY2sifQ.oEXwTsJiOq2da037fl2cbKKzPCq4iPYReQPnQA8ZtWxG" +
			"74D3CoYRCyPT6GrL-H8xi1PUI7AAvGmsJaStDZifPA";

		env.putString("vp_token", sdJwtStr);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("vp_token");
	}

}

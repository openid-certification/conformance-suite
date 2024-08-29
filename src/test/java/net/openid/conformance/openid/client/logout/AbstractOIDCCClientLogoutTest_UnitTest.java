package net.openid.conformance.openid.client.logout;

import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AbstractOIDCCClientLogoutTest_UnitTest {

	static class TestAbstractOIDCCClientLogoutTest extends AbstractOIDCCClientLogoutTest {
		TestAbstractOIDCCClientLogoutTest() {
			env.putString("base_url", "");
			env.putString("rp_frontchannel_logout_uri_request_url", "https://rp_frontchannel_logout_uri_request_url.com?alert('1')");
			env.putString("post_logout_redirect_uri_redirect", "");
		}
	}

	@Test
	void createFrontChannelLogoutModelAndView_properly_escapes_the_rp_frontchannel_logout_uri_request_url() {
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		TestAbstractOIDCCClientLogoutTest test = new TestAbstractOIDCCClientLogoutTest();
		test.setProperties("UNIT-TEST", Map.of("", ""), eventLog, null, infoService, null, null);

		ModelAndView mav = (ModelAndView) test.createFrontChannelLogoutModelAndView(false);
		Map<String, Object> model = mav.getModel();

		assertEquals("https:\\/\\/rp_frontchannel_logout_uri_request_url.com?alert(\\'1\\')", model.get("rp_frontchannel_logout_uri"));
	}
}

package net.openid.conformance.ui;

import net.openid.conformance.condition.common.util.StalledHttpServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for https://gitlab.com/openid/conformance-suite/-/work_items/1827: the external-IP
 * lookup done at startup must be bounded, so a slow or unresponsive endpoint returns null quickly
 * instead of blocking application startup.
 */
public class ServerInfoTemplate_UnitTest {

	@Test
	public void fetchExternalIp_returnsNullAndDoesNotHang_whenEndpointStalls() throws Exception {
		try (StalledHttpServer server = new StalledHttpServer(StalledHttpServer.Mode.ACCEPT_AND_HANG)) {
			Instant start = Instant.now();
			String ip = ServerInfoTemplate.fetchExternalIp(server.getUrl(), 1);
			Duration elapsed = Duration.between(start, Instant.now());

			assertThat(ip).isNull();
			assertThat(elapsed).isLessThan(Duration.ofSeconds(9));
		}
	}
}

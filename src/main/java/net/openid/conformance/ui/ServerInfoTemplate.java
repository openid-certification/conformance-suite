package net.openid.conformance.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.GitProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Static implementation of server info utility methods.
 */
@Component
public class ServerInfoTemplate {

	private static final Logger logger = LoggerFactory.getLogger(ServerInfoTemplate.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Value("${fintechlabs.show_external_ip_address}")
	private Boolean showExternalIpAddress;

	@Autowired(required = false)
	private GitProperties gitProperties;

	private static final String URI = "https://api.ipify.org/";
	// Timeout for the (best-effort) external-IP lookup, bounded so a slow or unresponsive endpoint
	// cannot block application startup. See https://gitlab.com/openid/conformance-suite/-/work_items/1827
	private static final int IP_LOOKUP_TIMEOUT_SECONDS = 10;
	private static final Map<String, String> SERVER_INFO = new HashMap<>();

	public Map<String, String> getServerInfo() {
		return SERVER_INFO;
	}

	public void initServerInfo() {
		SERVER_INFO.put("version", version);
		SERVER_INFO.put("external_ip", callServiceToGetExternalIpAddress());
		if (gitProperties != null) {
			SERVER_INFO.put("revision", gitProperties.getShortCommitId());
			SERVER_INFO.put("tag", gitProperties.get("closest.tag.name"));
			Instant buildTime = Instant.ofEpochMilli(Long.parseLong(gitProperties.get("build.time")));
			SERVER_INFO.put("build_time", buildTime.toString());
		}
	}

	/***
	 * Call Service To Get External Ip Address
	 * @return
	 */
	private String callServiceToGetExternalIpAddress() {
		if (showExternalIpAddress != null && showExternalIpAddress) {
			return fetchExternalIp(URI, IP_LOOKUP_TIMEOUT_SECONDS);
		}
		return null;
	}

	/**
	 * Best-effort external-IP lookup with bounded connect/read timeouts. Returns null (and logs) on any
	 * failure so a slow or unreachable endpoint can never block startup. Package-private for testing.
	 */
	static String fetchExternalIp(String url, int timeoutSeconds) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
		factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
		try {
			return new RestTemplate(factory).getForObject(url, String.class);
		} catch (RestClientException e) {
			logger.warn("Could not determine external IP address from {}", url, e);
			return null;
		}
	}
}

package net.openid.conformance.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Static implementation of server info utility methods.
 */
public class ServerInfoTemplate {
	@Value("${fintechlabs.version}")
	private String version;
	@Value("${fintechlabs.show_external_ip_address}")
	private Boolean showExternalIpAddress;

	private static final String URI = "https://api.ipify.org/";
	private static final Map<String, String> SERVER_INFO = new HashMap<>();

	public Map<String, String> getServerInfo(){
		return SERVER_INFO;
	}
	public void initServerInfo() {
		SERVER_INFO.put("version", version);
		SERVER_INFO.put("external_ip", callServiceToGetExternalIpAddress());
	}

	/***
	 * Call Service To Get External Ip Address
	 * @return
	 */
	private String callServiceToGetExternalIpAddress() {
		if (showExternalIpAddress) {
			RestTemplate restTemplate = new RestTemplate();
			return restTemplate.getForObject(URI, String.class);
		}
		return null;
	}
}

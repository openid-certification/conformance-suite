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
	
	@Value("${fintechlabs.external_ip_service_uri}")
	private String ipAddressServiceUri;

	@Value("${fintechlabs.issuer}")
	private String issuer;

	@Value("${fintechlabs.logo}")
	private String logo;

	@Value("${fintechlabs.brand}")
	private String brand;

	private static final Map<String, String> SERVER_INFO = new HashMap<>();

	public Map<String, String> getServerInfo(){
		return SERVER_INFO;
	}
	public void initServerInfo() {
		SERVER_INFO.put("version", version);
		SERVER_INFO.put("external_ip", callServiceToGetExternalIpAddress());
		SERVER_INFO.put("issuer", issuer);
		SERVER_INFO.put("logo", logo);
		SERVER_INFO.put("brand", brand);
	}

	/***
	 * Call Service To Get External Ip Address
	 * @return
	 */
	private String callServiceToGetExternalIpAddress() {
		if (showExternalIpAddress) {
			RestTemplate restTemplate = new RestTemplate();
			return restTemplate.getForObject(ipAddressServiceUri, String.class);
		}
		return null;
    }
}

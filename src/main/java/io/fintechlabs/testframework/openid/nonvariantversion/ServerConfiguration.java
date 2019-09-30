package io.fintechlabs.testframework.openid.nonvariantversion;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

/**
 * TODO tests should be able to get various runtime values from ServerConfiguration
 * for example supported algorithms, dynamic client registration options etc
 */
public class ServerConfiguration
{
	private static Logger logger = LoggerFactory.getLogger(ServerConfiguration.class);
	public enum DiscoveryType {DYNAMIC, STATIC};

	public DiscoveryType discoveryType;

	private JsonObject openidConfiguration;

	private boolean loadedConfig = false;

	/**
	 * use this for static config
	 * @param staticConfigJSON
	 */
	public ServerConfiguration(String staticConfigJSON)
	{
		this.discoveryType = DiscoveryType.STATIC;
		Gson gson = new GsonBuilder().create();
		try
		{
			this.openidConfiguration = new JsonParser().parse(staticConfigJSON).getAsJsonObject();
			loadedConfig = true;
		}
		catch (Exception ex)
		{
			logger.error("Error parsing static configuration");
			loadedConfig = false;
		}
	}

	public ServerConfiguration(DiscoveryType discoType, JsonObject serverConfigurationFromEnv) {
		this.discoveryType = discoType;
		this.loadedConfig = true;
		this.openidConfiguration = serverConfigurationFromEnv;
	}





	public JsonArray getArrayValueFromConfig(String configName)
	{
		if(loadedConfig && openidConfiguration!=null) {
			return openidConfiguration.getAsJsonArray(configName);
		}
		return null;
	}

	public String getStringValueFromConfig(String configName)
	{
		if(loadedConfig && openidConfiguration!=null) {
			return OIDFJSON.getString(openidConfiguration.getAsJsonPrimitive(configName));
		}
		return null;
	}
}

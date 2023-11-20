package net.openid.conformance.condition.as;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

public class ValidateClientCertificateForTlsClientAuth extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_certificate", "client"})
	public Environment evaluate(Environment env) {

		JsonObject certIfo = env.getObject("client_certificate");
		JsonObject client = env.getObject("client");
		int fieldCount = 0;
		/*
		A client using the
		"tls_client_auth" authentication method MUST use exactly one of the
		below metadata parameters to indicate the certificate subject value
		that the authorization server is to expect when authenticating the
		respective client.

		Also https://tools.ietf.org/html/rfc8705#section-2.1
			Only one subject name value of any type is used for each
			client.
		*/
		if(client.has("tls_client_auth_subject_dn")) {
			fieldCount++;
		}
		if(client.has("tls_client_auth_san_dns")) {
			fieldCount++;
		}
		if(client.has("tls_client_auth_san_uri")) {
			fieldCount++;
		}
		if(client.has("tls_client_auth_san_ip")) {
			fieldCount++;
		}
		if(client.has("tls_client_auth_san_email")) {
			fieldCount++;
		}
		if(fieldCount!=1) {
			throw error("Client must have only one of " +
						"tls_client_auth_subject_dn, tls_client_auth_san_dns, tls_client_auth_san_uri, " +
						"tls_client_auth_san_ip and tls_client_auth_san_email metadata values set" +
						"(cannot have more than one set)", args("client", client));
		}

		if(client.has("tls_client_auth_subject_dn")) {
			try {
				LdapName expectedDn = new LdapName(OIDFJSON.getString(client.get("tls_client_auth_subject_dn")));
				List<Rdn> expectedRDNs = expectedDn.getRdns();
				LdapName actualDn = new LdapName(OIDFJSON.getString(certIfo.get("subject").getAsJsonObject().get("dn")));
				List<Rdn> actualRDNs = actualDn.getRdns();
				//Compare DNs independent of RDN element order. Java and Openssl order them differently so 'equals' won't work
				if (expectedRDNs.size() == actualRDNs.size() && expectedRDNs.containsAll(actualRDNs)) {
					logSuccess("Certificate subject dn is valid", args("subject_dn", actualDn.toString()));
					return env;
				} else {
					throw error("Certificate subject dn in request does not match expected tls_client_auth_subject_dn",
								args("expected", expectedDn.toString(), "actual", actualDn.toString()));
				}
			} catch (InvalidNameException ex) {
				throw error("Invalid subject dn", ex,
							args("expected_dn", OIDFJSON.getString(client.get("tls_client_auth_subject_dn")),
								"actual_dn", OIDFJSON.getString(certIfo.get("subject").getAsJsonObject().get("dn")))
				);
			}
		}
		if(client.has("tls_client_auth_san_dns")) {
			String expected = OIDFJSON.getString(client.get("tls_client_auth_san_dns"));
			JsonArray actualValues = certIfo.get("sanDnsNames").getAsJsonArray();

			if(jsonArrayContainsCaseInsensitive(actualValues, expected)) {
				logSuccess("Certificate contains the expected tls_client_auth_san_dns",
							args("expected", expected, "actual", actualValues));
				return env;
			} else {
				throw error("Certificate does not contain the configured tls_client_auth_san_dns, " +
							"dNSName subject alternative name, entry",
							args("expected", expected, "actual", actualValues));
			}
		}
		if(client.has("tls_client_auth_san_uri")) {
			String expected = OIDFJSON.getString(client.get("tls_client_auth_san_uri"));
			JsonArray actualValues = certIfo.get("sanUris").getAsJsonArray();

			if(checkSanUri(actualValues, expected)) {
				logSuccess("Certificate contains the expected tls_client_auth_san_uri",
							args("expected", expected, "actual", actualValues));
				return env;
			} else {
				throw error("Certificate does not contain the configured tls_client_auth_san_uri," +
							" uniformResourceIdentifier subject alternative name, entry",
					args("expected", expected, "actual", actualValues));
			}
		}
		if(client.has("tls_client_auth_san_ip")) {
			String expected = OIDFJSON.getString(client.get("tls_client_auth_san_ip"));
			JsonArray actualValues = certIfo.get("sanIPs").getAsJsonArray();

			if(checkSanIP(actualValues, expected)) {
				logSuccess("Certificate contains the expected tls_client_auth_san_ip",
					args("expected", expected, "actual", actualValues));
				return env;
			} else {
				throw error("Certificate does not contain the configured tls_client_auth_san_ip, " +
							"iPAddress subject alternative name, entry",
					args("expected", expected, "actual", actualValues));
			}
		}
		if(client.has("tls_client_auth_san_email")) {
			String expected = OIDFJSON.getString(client.get("tls_client_auth_san_email"));
			JsonArray actualValues = certIfo.get("sanEmails").getAsJsonArray();

			if(jsonArrayContainsCaseInsensitive(actualValues, expected)) {
				logSuccess("Certificate contains the expected tls_client_auth_san_email",
					args("expected", expected, "actual", actualValues));
				return env;
			} else {
				throw error("Certificate does not contain the configured tls_client_auth_san_email, " +
							"rfc822Name subject alternative name, entry",
					args("expected", expected, "actual", actualValues));
			}
		}

		throw error("Client must contain one of tls_client_auth_subject_dn, tls_client_auth_san_dns, tls_client_auth_san_uri," +
					"tls_client_auth_san_ip or tls_client_auth_san_email metadata parameters");


	}

	public boolean checkSanUri(JsonArray jsonElements, String expected) {
		URI expectedURI = null;
		try {
			expectedURI = new URI(expected);
		} catch (URISyntaxException e) {
			throw error("Invalid expected URI", args("expected_uri", expected));
		}

		for(JsonElement element : jsonElements) {
			try {
				URI uri = new URI(OIDFJSON.getString(element));
				if(expectedURI.equals(uri)) {
					return true;
				}
			} catch (URISyntaxException e) {
				//TODO this one might be an invalid uri but in theory there may be another valid SAN value that will match the configured
				// should we throw an error or not?
				throw error("Invalid URI value in subject alternative names", args("invalid_uri", element));
			}
		}
		return false;
	}

	public boolean checkSanIP(JsonArray jsonElements, String expected) {
		InetAddress expectedAddress = InetAddresses.forString(expected);
		for(JsonElement element : jsonElements) {
			InetAddress address = InetAddresses.forString(OIDFJSON.getString(element));
			if(expectedAddress.equals(address)) {
				return true;
			}
		}
		return false;
	}

	private boolean jsonArrayContainsCaseInsensitive(JsonArray jsonElements, String expectedValue) {
		for(JsonElement element : jsonElements) {
			String elementValue = OIDFJSON.getString(element);
			if(elementValue.toLowerCase(Locale.ENGLISH).equals(expectedValue.toLowerCase(Locale.ENGLISH))) {
				return true;
			}
		}
		return false;
	}

}

package net.openid.conformance.extensions;

import net.openid.conformance.testmodule.AbstractTestModule;

import java.util.HashMap;
import java.util.Map;

public class AlternateKeystoreRegistry {

	private Map<String, KeystoreStrategy> mtlsStrategies = new HashMap<>();

	private AlternateKeystoreRegistry() {}

	private static final AlternateKeystoreRegistry INSTANCE = new AlternateKeystoreRegistry();

	public static AlternateKeystoreRegistry getINSTANCE() {
		return INSTANCE;
	}

	public void register(String providerName, KeystoreStrategy strategy) {
		mtlsStrategies.put(providerName, strategy);
	}

	public KeystoreStrategy forName(String name) {
		if(mtlsStrategies.containsKey(name)) {
			return mtlsStrategies.get(name);
		}
		throw new RuntimeException("No alternate mtls strategy called " + name + " is configured");
	}

}

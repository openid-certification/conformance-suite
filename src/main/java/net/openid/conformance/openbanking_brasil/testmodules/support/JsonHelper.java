package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class JsonHelper {

    static public boolean ifExists(JsonObject jsonObject, String path) {
		try {
			JsonPath.read(jsonObject, path);
			return true;
		} catch (PathNotFoundException | IllegalArgumentException ex) {
			return false;
		}

	}
    
}

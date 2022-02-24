package net.openid.conformance.openbanking_brasil.generic;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ChangeResponse extends AbstractJsonAssertingCondition {

	public Environment evaluate(Environment environment) {
		JsonObject response = environment.getObject("resource_endpoint_response_full");
		String jwt = OIDFJSON.getString(response.get("body"));
		String[] jwts = jwt.split("\\.");
		jwts[1] = "eyJhdWQiOiJiMDI0Y2Y0My05Y2U2LTUyYzQtODgyMC05YjVkMGJjY2I3NGMiLCJtZXRhIjp7InRvdGFsUmVjb3JkcyI6MSwicmVxdWVzdERhdGVUaW1lIjoiMjAyMi0wMi0yNFQwNzo1MTo0OFoiLCJ0b3RhbFBhZ2VzIjoxfSwiaXNzIjoiYjAyNGNmNDMtOWNlNi01MmM0LTg4MjAtOWI1ZDBiY2NiNzRjIiwiaWF0IjoxNjQ1Njk5OTA4LCJlcnJvcnMiOlt7ImNvZGUiOiJDT05TRU5USU1FTlRPX0lOVkFMSURPIiwiZGV0YWlsIjoiQ29uc2VudGltZW50byBpbnbDoWxpZG8gKHN0YXR1cyBkaWZlcmVudGUgZGUgQVVUSE9SSVNFRCBvdSBlc3TDoSBleHBpcmFkbykuIiwidGl0bGUiOiJDb25zZW50aW1lbnRvIGludsOhbGlkby4ifV0sImp0aSI6ImVkYjI1ZjNhLTY5OTUtNGIyOS04MzMwLTZmMDBiZjhhMjliMiJ9";
		jwt = String.join(".", jwts);
		response.addProperty("body", jwt);
		response.addProperty("status", 422);
		environment.putObject("resource_endpoint_response_full", response);
		environment.putInteger("resource_endpoint_response_status", 422);
		log(response);
		return null;
	}
}

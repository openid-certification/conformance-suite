package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public abstract class ValidateNumberOfRecords extends AbstractJsonAssertingCondition {

	protected int numberOfReturnedRecords;

	protected int pageSize;
	protected int totalNumberOfRecords;
	protected int totalNumberOfPages;
	private JsonObject linksObject;
	protected int currentPageNumber;
	protected String selfLink;

	public void prepareRecordData(Environment env) {

		JsonElement body = bodyFrom(env);

		System.out.println(body);

		JsonArray dataArray = findByPath(body, "$.data").getAsJsonArray();

		numberOfReturnedRecords = dataArray.size();

		linksObject = findByPath(body, "$.links").getAsJsonObject();
		selfLink = OIDFJSON.getString(findByPath(linksObject, "$.self"));

		currentPageNumber = getPageNumber(selfLink);
		pageSize = getPageSize(selfLink);

		JsonObject metaObject = findByPath(body, "$.meta").getAsJsonObject();

		totalNumberOfRecords = OIDFJSON.getInt(findByPath(metaObject, "$.totalRecords"));
		totalNumberOfPages = OIDFJSON.getInt(findByPath(metaObject, "$.totalPages"));

	}

	protected int getPageNumber(String uri) {
		try {
			List<NameValuePair> params = URLEncodedUtils.parse(new URI(uri), StandardCharsets.UTF_8);
			return Integer.parseInt(
				params.stream()
					.filter(p -> p.getName().equals("page"))
					.findFirst()
					.orElseThrow(() -> error("Page parameter is not found in the link", Map.of("Link", uri)))
					.getValue());

		} catch (URISyntaxException e) {
			throw error("Link is not a valid URI", Map.of("Link", uri));
		}
	}

	protected int getPageSize(String uri) {
		try {
			List<NameValuePair> params = URLEncodedUtils.parse(new URI(uri), StandardCharsets.UTF_8);
			return Integer.parseInt(
				params.stream()
					.filter(p -> p.getName().equals("page-size"))
					.findFirst()
					.orElse(new BasicNameValuePair("page-size", "25"))
					.getValue());

		} catch (URISyntaxException e) {
			throw error("Link is not a valid URI", Map.of("Link", uri));
		}
	}

	protected void validateLastLink() {
		String lastLink = OIDFJSON.getString(findByPath(linksObject, "$.last"));
		int lastLinkPageNumber = getPageNumber(lastLink);
		if (lastLinkPageNumber != totalNumberOfPages) {
			throw error("Page number in the last link does not match the value of the totalPages field in meta object",
				Map.of("Last link", lastLink,
					"Provided page number", lastLinkPageNumber,
					"Expected page number", totalNumberOfPages));
		} else {
			logSuccess("Last page number matches the number in the last link");
		}

	}

	protected void validateNextLink() {
		String nextLink = OIDFJSON.getString(findByPath(linksObject, "$.next"));
		int nextLinkPageNumber = getPageNumber(nextLink);
		if (nextLinkPageNumber != currentPageNumber + 1) {
			throw error("Page number in the next link is incorrect",
				Map.of("Next link", nextLink,
					"Provided page number", nextLinkPageNumber,
					"Expected page number", currentPageNumber + 1));
		} else {
			logSuccess("Next page number matches the number in the next link");
		}
	}

	protected void validatePrevLink() {
		String prevLink = OIDFJSON.getString(findByPath(linksObject, "$.prev"));
		int prevLinkPageNumber = getPageNumber(prevLink);

		if (prevLinkPageNumber != currentPageNumber - 1) {
			throw error("Page number in the prev link is incorrect",
				Map.of("Prev link", prevLink,
					"Provided page number", prevLinkPageNumber,
					"Expected page number", currentPageNumber - 1));
		}
	}

}

package net.openid.conformance.openbanking_brasil.testmodules.support;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.google.common.base.Strings;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

public class ValidateResponseMetaData extends AbstractJsonAssertingCondition {

    @Override
	@PreEnvironment(strings = "resource_endpoint_response")
    public Environment evaluate(Environment env) {

        JsonObject apiResponse = bodyFrom(env);
        JsonElement dataElement = findByPath(apiResponse, "$.data");
        int metaTotalRecords = OIDFJSON.getInt(findByPath(apiResponse, "$.meta.totalRecords"));
        int metaTotalPages = OIDFJSON.getInt(findByPath(apiResponse, "$.meta.totalPages"));
        //JsonElement metaRequestDateTime = findByPath(apiResponse, "$.data.meta.requestDateTime");

        String selfLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.self"));
        String nextLink = "";
        String prevLink = "";

        if (ifExists(apiResponse, "$.links.next")) {
            nextLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.next"));
        } 

        if (ifExists(apiResponse, "$.links.prev")) {
            prevLink = OIDFJSON.getString(findByPath(apiResponse, "$.links.prev"));
        }

        // Check if the record count in meta tallies with the actual data. 
        // i.e. if record count > 1, then we should find an array in the data element.

        int arrayCount = 1; // We'll assume there is at least one data element.
		if (dataElement.isJsonArray()) {
            arrayCount = dataElement.getAsJsonArray().size();
        } 
                
        if (arrayCount > metaTotalRecords) {
            throw error("Data contains more items than the metadata totalRecords.");
        }

        // check if there is 1 page - if so, there should not be a next and prev link.
        if (metaTotalPages == 1) {
            
            // Make sure we don't have a next or prev link
            if (!Strings.isNullOrEmpty(nextLink) || !Strings.isNullOrEmpty(prevLink) ) {

                throw error("There should not be a 'next' or 'prev' link.");
            }
        } else {

            // There is more than one page. Parse the self link
            URI selfLinkURI;
            try {
                selfLinkURI = new URI(selfLink);
            } catch (URISyntaxException e) {
                throw error("Invalid Self Link URI.");
            }

            List<NameValuePair> selfLinkParamList = URLEncodedUtils.parse(selfLinkURI, StandardCharsets.UTF_8);
            MultiValueMap<String, String> selfLinkQueryStringParams = convertQueryStringParamsToMap(selfLinkParamList);

            // if Self is page=1, then we should not see a prev link
            int selfLinkPageNum = 1;            
            try {
                selfLinkPageNum  = Integer.parseInt(selfLinkQueryStringParams.getFirst("page"));
            } catch (NumberFormatException e) {}

            if ( selfLinkPageNum == 1) {

                if (!Strings.isNullOrEmpty(prevLink) ) {

                    throw error("There should not be a 'prev' link.");
                }

                // self link page = 1, total page > 1 - we need a next link.
                if (Strings.isNullOrEmpty(nextLink) ) {
                    throw error("There should be a 'next' link.");
                }
            }

            if ( selfLinkPageNum > 1 && selfLinkPageNum < metaTotalPages) {
                // Total pages > 1 and self page > 1 and self page < total pages - so we should see a next & prev link
                if (Strings.isNullOrEmpty(nextLink) ) {
                    throw error("There should be a 'next' link.");
                }

                if (Strings.isNullOrEmpty(prevLink) ) {
                    throw error("There should be a 'prev' link.");
                }
            }

            // if Self page= metaTotalPages (i.e. we are on the last page), then we should not find a next link.
            if (selfLinkPageNum == metaTotalPages) {

                if (!Strings.isNullOrEmpty(nextLink) ) {

                    String errorMsg = "There should not be a 'next' link.";
                    throw error(errorMsg);
                }

                if (Strings.isNullOrEmpty(prevLink) ) {

                    String errorMsg = "There should be a 'prev' link.";
                    throw error(errorMsg);
                }
            }
        }
        
        return env;
	}    

    protected MultiValueMap<String, String> convertQueryStringParamsToMap(List<NameValuePair> parameters) {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		for (NameValuePair pair : parameters) {
			queryParams.add(pair.getName(), pair.getValue());
		}
		return queryParams;
    }

    private boolean ifExists(JsonObject jsonObject, String path) {
		try {
			JsonPath.read(jsonObject, path);
			return true;
		} catch (PathNotFoundException e) {
			return false;
		}
	}
}

package net.openid.conformance.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * This class is a minimalist template engine with fixed markup and evaluation syntaxes.
 *
 * The markup uses the anchor syntax of bash with "${" and "}" and whatever is inside that is mapped to an identifier.
 * The identifiers are registered on supportedAnchorProcessors maps, the mapped function is executed and the returning
 * value replaces the anchor on the content.
 *
 * if the anchor is not found on supportedAnchorProcessors map, the anchor itself is echoed on the result output.
 *
 * A sample of input would
 *
 * "richAuthorizationRequest": [
 *             {
 *                 "type": "urn:openfinanceuae:account-access-consent:v1.0",
 *                 "Consent": {
 *                     "OnBehalfOf": {
 *                         "TradingName": "${name}",
 *                         "LegalName": "${name}-CBUAE",
 *                         "IdentifierType": "Other",
 *                         "Identifier": "Identifier"
 *                     },
 *                     "ConsentId": "${uuid}",
 *                     "Permissions": [
 *                         "ReadAccountsBasic",
 *                         "ReadProduct"
 *                     ]
 *                 }
 *             }
 *         ]
 *
 * That would be output to a structure like:
 *
 * "richAuthorizationRequest": [
 *             {
 *                 "type": "urn:openfinanceuae:account-access-consent:v1.0",
 *                 "Consent": {
 *                     "OnBehalfOf": {
 *                         "TradingName": "AbnshrFSeF",
 *                         "LegalName": "sofhkwlffjf-CBUAE",
 *                         "IdentifierType": "Other",
 *                         "Identifier": "Identifier"
 *                     },
 *                     "ConsentId": "c654ac8d-659f-46cc-8830-915ae7d63d66",
 *                     "Permissions": [
 *                         "ReadAccountsBasic",
 *                         "ReadProduct"
 *                     ]
 *                 }
 *             }
 *         ]
 *
 * Extending the anchors supported does not require changing this class and potential extension would be: integrating
 * faker or allowing rest calls to be described to retrieve.
 *
 * it is a top-level parser that does not support recursions.
 *
 * Methods for traversing the Gson objects and producing a new graph with replaced the anchors is missing.
 */
public class TemplateProcessor {

	/**
	 * Map with a list of anchors that are supported.
	 * it is public to allow other anchors to be introduced without changing this class
	 */
	public static Map<String, BiFunction<String, Object,String>> supportedAnchorProcessors = new HashMap<>();


	static {
		supportedAnchorProcessors.put("uuid", TemplateProcessor::uuid);
		supportedAnchorProcessors.put("id", TemplateProcessor::id);
		supportedAnchorProcessors.put("name", TemplateProcessor::name);
	}

	public static String uuid(String name, Object env){
		return UUID.randomUUID().toString();
	}
	public static String id(String name, Object env){
		return RandomStringUtils.secure().next(10,false,true);
	}

	public static String name(String name, Object env){
		return RandomStringUtils.secure().next(10,true,false);
	}


	/**
	 * this helper method replaces the anchors on content to processed values
	 *  anchors starts with "${" string and ends with "}"
	 * @param content
	 * @return
	 */
	public static String process(String content){
		StringBuilder sb = new StringBuilder();
		char[] cnt = content.toCharArray();
		internalProcess(cnt, 0, sb);
		return sb.toString();
	}

	private static void internalProcess(char[] cnt, int pos, StringBuilder sb){
		int index = pos;
		while (index < cnt.length - 3 && !(cnt[index] == '$' && cnt[index+1] == '{')) {
			index++;
		}
		if (cnt[index] != '$'){
			// parser hit the end of string
			sb.append(cnt, pos, cnt.length - pos);
			return;
		}
		sb.append(cnt, pos,  index - pos);
		int start = index +2 ;

		int end = start;
		while (end < cnt.length && !(cnt[end] == '}')) {
			end++;
		}

		String anchor = new String(cnt,start, end - start);
		if(supportedAnchorProcessors.containsKey(anchor)){
			sb.append(supportedAnchorProcessors.get(anchor).apply(anchor,null));
		} else {
			sb.append("${");
			sb.append(anchor);
			sb.append("}");
		}
		internalProcess(cnt, end + 1,sb);
	}
}

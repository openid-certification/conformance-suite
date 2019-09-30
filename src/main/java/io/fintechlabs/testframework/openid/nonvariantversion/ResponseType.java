package io.fintechlabs.testframework.openid.nonvariantversion;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
public class ResponseType
{
	public static final String CODE="code";
	public static final String ID_TOKEN="id_token";
	public static final String TOKEN="token";

	private boolean codeEnabled = false;
	private boolean tokenEnabled = false;
	private boolean idTokenEnabled = false;


	public ResponseType(boolean useCode, boolean useToken, boolean useIdToken)
	{
		this.codeEnabled = useCode;
		this.tokenEnabled = useToken;
		this.idTokenEnabled = useIdToken;
	}

	public ResponseType(String valueFromConfig)
	{
		if(valueFromConfig==null || valueFromConfig.isEmpty()) {
			this.codeEnabled = true;
			return;
		}
		String[] splitValue = valueFromConfig.split(" ");
		Set<String> valuesSet = new HashSet<>();
		Collections.addAll(valuesSet, splitValue);
		if(valuesSet.contains(CODE)) {
			this.codeEnabled = true;
		}
		if(valuesSet.contains(ID_TOKEN)) {
			this.idTokenEnabled = true;
		}
		if(valuesSet.contains(TOKEN)) {
			this.tokenEnabled = true;
		}
	}

	public String getAsString()
	{
		Set<String> values = new LinkedHashSet<>();
		if(codeEnabled)
		{
			values.add(CODE);
		}
		if(idTokenEnabled)
		{
			values.add(ID_TOKEN);
		}
		if(tokenEnabled)
		{
			values.add(TOKEN);
		}
		return String.join(" ", values);
	}

	public ResponseMode.ResponseModeValue getDefaultResponseMode()
	{
		if(tokenEnabled || idTokenEnabled)
		{
			return ResponseMode.ResponseModeValue.fragment;
		}
		return ResponseMode.ResponseModeValue.query;
	}

	public boolean includesCode()
	{
		return this.codeEnabled;
	}

	public boolean includesToken()
	{
		return this.tokenEnabled;
	}

	public boolean includesIdToken()
	{
		return this.idTokenEnabled;
	}
}

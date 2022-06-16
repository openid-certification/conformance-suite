package net.openid.conformance.openbanking_brasil.testmodules.support.payments.pixqrcode;

import org.apache.commons.lang3.StringUtils;

public class Tag {
	private String id;
	private String value;

	public Tag(final String id, final String value){
		this.id = id;
		this.value = value;
	}

	public void setId(String id){ this.id = id; }
	public String getId() { return id; }

	public void setValue(String value){ this.value = value; }
	public String getValue() { return value; }

	@Override
	public String toString(){

		if(StringUtils.isBlank(value)){
			return StringUtils.EMPTY;
		}

		return String.format("%s%02d%s", id, value.length(), value);
	}

}

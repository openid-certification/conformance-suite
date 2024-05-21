package net.openid.conformance.support.mitre.compat.oidc;

import com.google.gson.JsonObject;

import java.io.Serializable;


public interface UserInfo extends Serializable {

	/**
	 * @return the userId
	 */
	String getSub();

	/**
	 * @param sub the userId to set
	 */
	void setSub(String sub);

	/**
	 * @return the preferred username
	 */
	String getPreferredUsername();

	/**
	 * @param preferredUsername the preferredUsername to set
	 */
	void setPreferredUsername(String preferredUsername);

	/**
	 * @return the name
	 */
	String getName();

	/**
	 * @param name the name to set
	 */
	void setName(String name);

	/**
	 * @return the givenName
	 */
	String getGivenName();

	/**
	 * @param givenName the givenName to set
	 */
	void setGivenName(String givenName);

	/**
	 * @return the familyName
	 */
	String getFamilyName();

	/**
	 * @param familyName the familyName to set
	 */
	void setFamilyName(String familyName);

	/**
	 * @return the middleName
	 */
	String getMiddleName();

	/**
	 * @param middleName the middleName to set
	 */
	void setMiddleName(String middleName);

	/**
	 * @return the nickname
	 */
	String getNickname();

	/**
	 * @param nickname the nickname to set
	 */
	void setNickname(String nickname);

	/**
	 * @return the profile
	 */
	String getProfile();

	/**
	 * @param profile the profile to set
	 */
	void setProfile(String profile);

	/**
	 * @return the picture
	 */
	String getPicture();

	/**
	 * @param picture the picture to set
	 */
	void setPicture(String picture);

	/**
	 * @return the website
	 */
	String getWebsite();

	/**
	 * @param website the website to set
	 */
	void setWebsite(String website);

	/**
	 * @return the email
	 */
	String getEmail();

	/**
	 * @param email the email to set
	 */
	void setEmail(String email);

	/**
	 * @return the verified
	 */
	Boolean getEmailVerified();

	/**
	 * @param emailVerified the verified to set
	 */
	void setEmailVerified(Boolean emailVerified);

	/**
	 * @return the gender
	 */
	String getGender();

	/**
	 * @param gender the gender to set
	 */
	void setGender(String gender);

	/**
	 * @return the zoneinfo
	 */
	String getZoneinfo();

	/**
	 * @param zoneinfo the zoneinfo to set
	 */
	void setZoneinfo(String zoneinfo);

	/**
	 * @return the locale
	 */
	String getLocale();

	/**
	 * @param locale the locale to set
	 */
	void setLocale(String locale);

	/**
	 * @return the phoneNumber
	 */
	String getPhoneNumber();

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	void setPhoneNumber(String phoneNumber);

	/**
	 *
	 */
	Boolean getPhoneNumberVerified();

	/**
	 *
	 * @param phoneNumberVerified
	 */
	void setPhoneNumberVerified(Boolean phoneNumberVerified);

	/**
	 * @return the address
	 */
	Address getAddress();

	/**
	 * @param address the address to set
	 */
	void setAddress(Address address);

	/**
	 * @return the updatedTime
	 */
	String getUpdatedTime();

	/**
	 * @param updatedTime the updatedTime to set
	 */
	void setUpdatedTime(String updatedTime);


	/**
	 *
	 * @return
	 */
	String getBirthdate();

	/**
	 *
	 * @param birthdate
	 */
	void setBirthdate(String birthdate);

	/**
	 * Serialize this UserInfo object to JSON.
	 *
	 * @return
	 */
	JsonObject toJson();

	/**
	 * The JSON source of this UserInfo (if it was fetched), or null if it's local.
	 * @return
	 */
	JsonObject getSource();

}

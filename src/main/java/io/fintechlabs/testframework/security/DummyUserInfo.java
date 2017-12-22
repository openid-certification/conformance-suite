package io.fintechlabs.testframework.security;

import com.google.gson.JsonObject;
import org.mitre.openid.connect.model.Address;
import org.mitre.openid.connect.model.UserInfo;

/**
 * This is a Dummy UserInfo object to provide an email address for the Dummy User when in Dev Mode.
 */
public class DummyUserInfo implements UserInfo {

	@Override
	public String getSub() {
		return "developer";
	}

	@Override
	public void setSub(String sub) {

	}

	@Override
	public String getPreferredUsername() {
		return null;
	}

	@Override
	public void setPreferredUsername(String preferredUsername) {

	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {

	}

	@Override
	public String getGivenName() {
		return null;
	}

	@Override
	public void setGivenName(String givenName) {

	}

	@Override
	public String getFamilyName() {
		return null;
	}

	@Override
	public void setFamilyName(String familyName) {

	}

	@Override
	public String getMiddleName() {
		return null;
	}

	@Override
	public void setMiddleName(String middleName) {

	}

	@Override
	public String getNickname() {
		return null;
	}

	@Override
	public void setNickname(String nickname) {

	}

	@Override
	public String getProfile() {
		return null;
	}

	@Override
	public void setProfile(String profile) {

	}

	@Override
	public String getPicture() {
		return null;
	}

	@Override
	public void setPicture(String picture) {

	}

	@Override
	public String getWebsite() {
		return null;
	}

	@Override
	public void setWebsite(String website) {

	}

	@Override
	public String getEmail() {
		return "developer@developer.com";
	}

	@Override
	public void setEmail(String email) {

	}

	@Override
	public Boolean getEmailVerified() {
		return null;
	}

	@Override
	public void setEmailVerified(Boolean emailVerified) {

	}

	@Override
	public String getGender() {
		return null;
	}

	@Override
	public void setGender(String gender) {

	}

	@Override
	public String getZoneinfo() {
		return null;
	}

	@Override
	public void setZoneinfo(String zoneinfo) {

	}

	@Override
	public String getLocale() {
		return null;
	}

	@Override
	public void setLocale(String locale) {

	}

	@Override
	public String getPhoneNumber() {
		return null;
	}

	@Override
	public void setPhoneNumber(String phoneNumber) {

	}

	@Override
	public Boolean getPhoneNumberVerified() {
		return null;
	}

	@Override
	public void setPhoneNumberVerified(Boolean phoneNumberVerified) {

	}

	@Override
	public Address getAddress() {
		return null;
	}

	@Override
	public void setAddress(Address address) {

	}

	@Override
	public String getUpdatedTime() {
		return null;
	}

	@Override
	public void setUpdatedTime(String updatedTime) {

	}

	@Override
	public String getBirthdate() {
		return null;
	}

	@Override
	public void setBirthdate(String birthdate) {

	}

	@Override
	public JsonObject toJson() {
		return null;
	}

	@Override
	public JsonObject getSource() {
		return null;
	}
}

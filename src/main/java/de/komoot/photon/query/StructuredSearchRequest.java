package de.komoot.photon.query;

import org.apache.commons.lang3.StringUtils;

public class StructuredSearchRequest extends SearchRequestBase {
    private String countryCode;
    private String state;
    private String county;
    private String city;
    private String postCode;
    private String district;
    private String street;
    private String houseNumber;

    public boolean getDebug() {
        return false;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean hasState() { return !StringUtils.isEmpty(this.state); }

    public boolean hasDistrict() { return !StringUtils.isEmpty(this.district); }

    public boolean hasPostCode() { return !StringUtils.isEmpty(this.postCode); }

    public boolean hasCityOrPostCode() { return !StringUtils.isEmpty(this.city) || hasPostCode(); }

    public boolean hasCounty() { return !StringUtils.isEmpty(this.county); }

    public boolean hasStreet() { return !StringUtils.isEmpty(this.street) || hasHouseNumber(); }

    public boolean hasHouseNumber() { return !StringUtils.isEmpty(this.houseNumber); }
}

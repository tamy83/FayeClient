/**
 * Created by ytam on 4/24/15.
 */
package com.monmouth.monmouthtelecom;

public class MobileCarrier {

    private String carrierName;
    private String countryCode;
    private int mcc;
    private int mnc;

    public MobileCarrier(String carrierName, String countryCode, int mcc, int mnc) {
        this.carrierName = carrierName;
        this.countryCode = countryCode;
        this.mcc = mcc;
        this.mnc = mnc;
    }

    public MobileCarrier() {

    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public String toString() {
        return "Carrier: " + carrierName + " country code: " + countryCode + " mcc: " + mcc + " mnc: " + mnc;
    }

    public String convertPhoneNumber(String num) {
        // US
        if (mcc == 310) {
            // t-mobile
            if (mnc == 26 || mnc == 60 || mnc == 160 || mnc == 260 || mnc == 490) {
                num = "+1" + num;
            }
        }
        return num;
    }
}

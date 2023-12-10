package org.geelato.web.platform;


public abstract class PlatformException{
    private int code;
    private String  fixSuggestion;
    private String documentLink;


    public abstract void setCode(int code) ;

    public int getCode() {
        return code;
    }

    public String getDocumentLink() {
        return documentLink;
    }

    public String getFixSuggestion() {
        return fixSuggestion;
    }

}

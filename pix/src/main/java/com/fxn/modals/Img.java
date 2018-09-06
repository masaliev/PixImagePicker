package com.fxn.modals;

import java.io.Serializable;

/**
 * Created by akshay on 17/03/18.
 */

public class Img implements Serializable {
    private String headerDate;
    private String contentUrl;
    private String url;
    private Boolean isSelected;

    public Img(String headerDate, String contentUrl, String url) {
        this.headerDate = headerDate;
        this.contentUrl = contentUrl;
        this.url = url;
        this.isSelected = false;
    }


    public String getHeaderDate() {
        return headerDate;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getUrl() {
        return url;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Img && contentUrl != null && contentUrl.length() > 0
                && contentUrl.equals(((Img) obj).contentUrl);
    }
}

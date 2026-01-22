package org.hytalewiki.net.response;

public class PageObject {
    private int id;

    private String key;
    private String title;
    private Latest latest;
    private String contentModel;
    private License license;

    private String htmlUrl;
    private String html;
    private String source;

    public PageObject() {
        // gson
    }

    public static class License {
        private String url;
        private String title;

        public License() {
            // gson
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class Latest {
        private int id;
        private String timestamp;

        public Latest() {
            // gson
        }

        public int getId() {
            return id;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public Latest getLatest() {
        return latest;
    }

    public String getContentModel() {
        return contentModel;
    }

    public License getLicense() {
        return license;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getHtml() {
        return html;
    }

    public String getSource() {
        return source;
    }
}

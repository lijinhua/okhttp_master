package okhttp3.test.intercept;

public class MyRequest {

    private String url;

    public MyRequest(String url){
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}

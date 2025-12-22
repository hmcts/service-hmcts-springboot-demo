package uk.gov.hmcts.cp.client;

public class HttpClient {

    public void getHttpRequest(String url) {
        // This method will accept or default all the required info such as
        // url, headers etc
        // And return a string or maybe an object to the caller.
        // Maybe a http response. Maybne dependes on our client implementation

        // We need to consider how this would work if we are called a openapi client
        // Which we will for notifications, we will create a spec for the subscriber to implement
        // But we will try and keep it as simple as possible in this demo
    }
}

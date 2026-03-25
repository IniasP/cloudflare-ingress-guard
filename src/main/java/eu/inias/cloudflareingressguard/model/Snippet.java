package eu.inias.cloudflareingressguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single NGINX configuration snippet for a SnippetsFilter.
 * The context determines where in the generated NGINX config the snippet is injected.
 */
public class Snippet {

    /**
     * The NGINX context to insert the snippet into.
     * Allowed values: main, http, http.server, http.server.location
     */
    @JsonProperty("context")
    private String context;

    /**
     * The raw NGINX configuration snippet value.
     */
    @JsonProperty("value")
    private String value;

    public Snippet() {
    }

    public Snippet(String context, String value) {
        this.context = context;
        this.value = value;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

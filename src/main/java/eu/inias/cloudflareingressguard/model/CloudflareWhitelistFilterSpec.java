package eu.inias.cloudflareingressguard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spec for the {@code CloudflareWhitelistFilter} CRD.
 *
 * <p>All fields are optional. When {@code snippetsFilterName} is omitted, the operator
 * uses the name of the {@code CloudflareWhitelistFilter} instance as the name of the
 * managed {@code SnippetsFilter}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudflareWhitelistFilterSpec {

    /**
     * Optional name for the managed {@code gateway.nginx.org/v1alpha1 SnippetsFilter}.
     * Defaults to the name of the {@code CloudflareWhitelistFilter} instance.
     */
    @JsonProperty("snippetsFilterName")
    private String snippetsFilterName;

    public CloudflareWhitelistFilterSpec() {
    }

    public CloudflareWhitelistFilterSpec(String snippetsFilterName) {
        this.snippetsFilterName = snippetsFilterName;
    }

    public String getSnippetsFilterName() {
        return snippetsFilterName;
    }

    public void setSnippetsFilterName(String snippetsFilterName) {
        this.snippetsFilterName = snippetsFilterName;
    }
}

package eu.inias.cloudflareingressguard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status for the {@code CloudflareWhitelistFilter} CRD.
 * Updated by the operator after each reconciliation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudflareWhitelistFilterStatus {

    /**
     * The name of the {@code SnippetsFilter} resource that is being managed.
     */
    @JsonProperty("snippetsFilterName")
    private String snippetsFilterName;

    public CloudflareWhitelistFilterStatus() {
    }

    public String getSnippetsFilterName() {
        return snippetsFilterName;
    }

    public void setSnippetsFilterName(String snippetsFilterName) {
        this.snippetsFilterName = snippetsFilterName;
    }
}

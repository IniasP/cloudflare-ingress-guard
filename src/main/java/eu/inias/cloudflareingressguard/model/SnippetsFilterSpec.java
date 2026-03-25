package eu.inias.cloudflareingressguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Spec for the NGF SnippetsFilter CRD.
 * Contains a list of NGINX configuration snippets (one per context, max 4).
 */
public class SnippetsFilterSpec {

    @JsonProperty("snippets")
    private List<Snippet> snippets;

    public SnippetsFilterSpec() {
    }

    public SnippetsFilterSpec(List<Snippet> snippets) {
        this.snippets = snippets;
    }

    public List<Snippet> getSnippets() {
        return snippets;
    }

    public void setSnippets(List<Snippet> snippets) {
        this.snippets = snippets;
    }
}

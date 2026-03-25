package eu.inias.cloudflareingressguard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Status for the NGF SnippetsFilter CRD.
 * Managed by NGINX Gateway Fabric; we do not write to this.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnippetsFilterStatus {
    // Status is managed by NGF; no fields needed for our reconciler.
}

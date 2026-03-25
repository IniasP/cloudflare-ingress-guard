package eu.inias.cloudflareingressguard.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * Fabric8 typed model for the NGINX Gateway Fabric SnippetsFilter CRD.
 *
 * <p>API: {@code gateway.nginx.org/v1alpha1 SnippetsFilter}
 *
 * <p>SnippetsFilter allows inserting raw NGINX configuration snippets into the
 * generated NGINX config for HTTPRoute resources. It is referenced from an
 * HTTPRoute rule via an {@code extensionRef} filter of type {@code ExtensionRef}.
 *
 * @see <a href="https://docs.nginx.com/nginx-gateway-fabric/reference/api/#gateway.nginx.org/v1alpha1.SnippetsFilter">NGF API reference</a>
 */
@Group("gateway.nginx.org")
@Version("v1alpha1")
@Kind("SnippetsFilter")
public class SnippetsFilter extends CustomResource<SnippetsFilterSpec, SnippetsFilterStatus>
        implements Namespaced {
}

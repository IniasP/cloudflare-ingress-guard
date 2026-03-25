package eu.inias.cloudflareingressguard.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * A {@code CloudflareWhitelistFilter} instructs the operator to create and maintain a
 * {@code gateway.nginx.org/v1alpha1 SnippetsFilter} in the same namespace, populated
 * with NGINX {@code allow} directives for all current Cloudflare IP ranges followed by
 * {@code deny all}.
 *
 * <p>The produced {@code SnippetsFilter} can then be referenced from any
 * {@code HTTPRoute} rule in the same namespace via an {@code ExtensionRef} filter,
 * restricting access to Cloudflare-proxied traffic only.
 *
 * <p>Example:
 * <pre>{@code
 * apiVersion: cloudflareingressguard.inias.eu/v1alpha1
 * kind: CloudflareWhitelistFilter
 * metadata:
 *   name: cloudflare-ips
 *   namespace: my-app
 * }</pre>
 *
 * <p>This creates a {@code SnippetsFilter} named {@code cloudflare-ips} in namespace
 * {@code my-app}. Reference it from an {@code HTTPRoute}:
 *
 * <pre>{@code
 * filters:
 *   - type: ExtensionRef
 *     extensionRef:
 *       group: gateway.nginx.org
 *       kind: SnippetsFilter
 *       name: cloudflare-ips
 * }</pre>
 */
@Group("cloudflareingressguard.inias.eu")
@Version("v1alpha1")
@Kind("CloudflareWhitelistFilter")
public class CloudflareWhitelistFilter
        extends CustomResource<CloudflareWhitelistFilterSpec, CloudflareWhitelistFilterStatus>
        implements Namespaced {
}

package eu.inias.cloudflareingressguard.reconcilers;

import eu.inias.cloudflareingressguard.model.*;
import eu.inias.cloudflareingressguard.services.CloudflareIpsService;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Reconciler for {@code cloudflareingressguard.inias.eu/v1alpha1 CloudflareWhitelistFilter}.
 *
 * <p>For each {@code CloudflareWhitelistFilter} instance, this reconciler creates and
 * maintains a {@code gateway.nginx.org/v1alpha1 SnippetsFilter} in the same namespace.
 * The {@code SnippetsFilter} contains NGINX {@code allow} directives for all current
 * Cloudflare IP ranges followed by {@code deny all} in the {@code http.server.location}
 * context.
 *
 * <p>The name of the produced {@code SnippetsFilter} is determined by
 * {@code spec.snippetsFilterName} if set, otherwise it defaults to the name of the
 * {@code CloudflareWhitelistFilter} instance.
 *
 * <p>The {@code SnippetsFilter} is owned by the {@code CloudflareWhitelistFilter} via an
 * owner reference, so it is automatically garbage-collected when the
 * {@code CloudflareWhitelistFilter} is deleted.
 *
 * <p>Users reference the produced {@code SnippetsFilter} from their {@code HTTPRoute}
 * rules via an {@code ExtensionRef} filter:
 *
 * <pre>{@code
 * filters:
 *   - type: ExtensionRef
 *     extensionRef:
 *       group: gateway.nginx.org
 *       kind: SnippetsFilter
 *       name: <snippetsFilterName>
 * }</pre>
 */
@Component
@ControllerConfiguration(
        maxReconciliationInterval = @MaxReconciliationInterval(interval = 7, timeUnit = TimeUnit.DAYS),
        generationAwareEventProcessing = false
)
public class CloudflareWhitelistFilterReconciler implements Reconciler<CloudflareWhitelistFilter> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareWhitelistFilterReconciler.class);

    private static final String NGINX_CONTEXT = "http.server.location";

    private final CloudflareIpsService cloudflareIpsService;
    private final KubernetesClient kubernetesClient;

    public CloudflareWhitelistFilterReconciler(CloudflareIpsService cloudflareIpsService,
                                               KubernetesClient kubernetesClient) {
        this.cloudflareIpsService = cloudflareIpsService;
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public UpdateControl<CloudflareWhitelistFilter> reconcile(
            CloudflareWhitelistFilter resource, Context<CloudflareWhitelistFilter> context) {

        String namespace = resource.getMetadata().getNamespace();
        String name = resource.getMetadata().getName();
        String resourceId = namespace + "/" + name;

        String snippetsFilterName = resolveSnippetsFilterName(resource);
        String desiredSnippetValue = buildNginxSnippet(cloudflareIpsService.getCachedCloudflareIps());

        boolean changed = applySnippetsFilter(resource, namespace, snippetsFilterName, desiredSnippetValue);

        if (changed) {
            LOGGER.info("Updated SnippetsFilter {}/{} for CloudflareWhitelistFilter {}.",
                    namespace, snippetsFilterName, resourceId);
        } else {
            LOGGER.info("SnippetsFilter {}/{} for CloudflareWhitelistFilter {} is up to date.",
                    namespace, snippetsFilterName, resourceId);
        }

        // Update status with the managed SnippetsFilter name
        if (resource.getStatus() == null
                || !Objects.equals(resource.getStatus().getSnippetsFilterName(), snippetsFilterName)) {
            CloudflareWhitelistFilterStatus status = new CloudflareWhitelistFilterStatus();
            status.setSnippetsFilterName(snippetsFilterName);
            resource.setStatus(status);
            return UpdateControl.patchStatus(resource);
        }

        return UpdateControl.noUpdate();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the name to use for the managed {@code SnippetsFilter}.
     * Uses {@code spec.snippetsFilterName} if set, otherwise the CRD instance name.
     */
    private String resolveSnippetsFilterName(CloudflareWhitelistFilter resource) {
        if (resource.getSpec() != null
                && resource.getSpec().getSnippetsFilterName() != null
                && !resource.getSpec().getSnippetsFilterName().isBlank()) {
            return resource.getSpec().getSnippetsFilterName();
        }
        return resource.getMetadata().getName();
    }

    /**
     * Builds the NGINX snippet: one {@code allow} directive per Cloudflare CIDR,
     * followed by {@code deny all}.
     */
    private String buildNginxSnippet(List<String> cloudflareIps) {
        return cloudflareIps.stream()
                .map(ip -> "allow " + ip + ";")
                .collect(Collectors.joining("\n", "", "\ndeny all;"));
    }

    /**
     * Creates or updates the {@code SnippetsFilter} in the cluster.
     *
     * @return {@code true} if the resource was created or updated
     */
    private boolean applySnippetsFilter(CloudflareWhitelistFilter owner,
                                        String namespace,
                                        String snippetsFilterName,
                                        String snippetValue) {
        SnippetsFilter existing = kubernetesClient
                .resources(SnippetsFilter.class)
                .inNamespace(namespace)
                .withName(snippetsFilterName)
                .get();

        if (existing == null) {
            LOGGER.info("Creating SnippetsFilter {}/{}.", namespace, snippetsFilterName);
            kubernetesClient.resources(SnippetsFilter.class)
                    .inNamespace(namespace)
                    .resource(buildSnippetsFilter(owner, namespace, snippetsFilterName, snippetValue))
                    .create();
            return true;
        }

        String existingValue = extractSnippetValue(existing);
        if (!Objects.equals(existingValue, snippetValue)) {
            LOGGER.info("Updating SnippetsFilter {}/{} with refreshed Cloudflare IPs.",
                    namespace, snippetsFilterName);
            existing.setSpec(new SnippetsFilterSpec(
                    List.of(new Snippet(NGINX_CONTEXT, snippetValue))));
            kubernetesClient.resources(SnippetsFilter.class)
                    .inNamespace(namespace)
                    .resource(existing)
                    .update();
            return true;
        }

        return false;
    }

    private SnippetsFilter buildSnippetsFilter(CloudflareWhitelistFilter owner,
                                               String namespace,
                                               String snippetsFilterName,
                                               String snippetValue) {
        SnippetsFilter sf = new SnippetsFilter();
        sf.setMetadata(new ObjectMetaBuilder()
                .withName(snippetsFilterName)
                .withNamespace(namespace)
                .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .withBlockOwnerDeletion(true)
                        .withController(true)
                        .build())
                .build());
        sf.setSpec(new SnippetsFilterSpec(List.of(new Snippet(NGINX_CONTEXT, snippetValue))));
        return sf;
    }

    private String extractSnippetValue(SnippetsFilter sf) {
        if (sf.getSpec() == null || sf.getSpec().getSnippets() == null) {
            return null;
        }
        return sf.getSpec().getSnippets().stream()
                .filter(s -> NGINX_CONTEXT.equals(s.getContext()))
                .map(Snippet::getValue)
                .findFirst()
                .orElse(null);
    }
}

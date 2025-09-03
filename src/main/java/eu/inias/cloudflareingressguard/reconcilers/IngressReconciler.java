package eu.inias.cloudflareingressguard.reconcilers;

import eu.inias.cloudflareingressguard.services.CloudflareIpsService;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ControllerConfiguration(
        maxReconciliationInterval = @MaxReconciliationInterval(interval = 7, timeUnit = TimeUnit.DAYS)
)
public class IngressReconciler implements Reconciler<Ingress> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngressReconciler.class);

    private static final String CF_GUARD_ANNOTATION = "cloudflare-ingress-guard.inias.eu/enabled";
    private static final String NGINX_WHITELIST_ANNOTATION = "nginx.ingress.kubernetes.io/whitelist-source-range";

    private final CloudflareIpsService cloudflareIpsService;

    public IngressReconciler(CloudflareIpsService cloudflareIpsService) {
        this.cloudflareIpsService = cloudflareIpsService;
    }

    @Override
    public UpdateControl<Ingress> reconcile(Ingress ingress, Context<Ingress> context) {
        String ingressIdentifier = ingress.getMetadata().getNamespace() + "/" + ingress.getMetadata().getName();

        Map<String, String> annotations = ingress.getMetadata().getAnnotations();
        if (annotations == null || !"true".equals(annotations.get(CF_GUARD_ANNOTATION))) {
            LOGGER.warn("Unprotected ingress {}.", ingressIdentifier);
            return UpdateControl.noUpdate();
        }

        String current = String.join(",", cloudflareIpsService.getCachedCloudflareIps());
        String previous = annotations.put(
                NGINX_WHITELIST_ANNOTATION,
                current
        );
        if (previous == null || !previous.equals(current)) {
            LOGGER.info("Updating IP whitelist for ingress {}: {}.", ingressIdentifier, current);
        } else {
            LOGGER.info("Ingress {} already protected.", ingressIdentifier);
            return UpdateControl.noUpdate();
        }

        ingress.getMetadata().setAnnotations(annotations);

        return UpdateControl.patchResource(ingress);
    }
}

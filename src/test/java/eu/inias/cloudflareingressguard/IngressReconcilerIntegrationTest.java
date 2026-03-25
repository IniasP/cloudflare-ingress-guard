package eu.inias.cloudflareingressguard;

import eu.inias.cloudflareingressguard.services.CloudflareIpsService;
import eu.inias.cloudflareingressguard.testconfig.OperatorTestConfiguration;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.springboot.starter.test.EnableMockOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static java.time.Duration.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableMockOperator(crdPaths = {
        "classpath:META-INF/fabric8/snippetsfilters.gateway.nginx.org-v1.yml",
        "classpath:crd/httproutes.gateway.networking.k8s.io.yaml"
})
@Import(OperatorTestConfiguration.class)
class IngressReconcilerIntegrationTest {

    static final String NAMESPACE = "test";
    static final List<String> TEST_IPS = List.of("103.21.244.0/22", "103.22.200.0/22", "2400:cb00::/32");
    static final String EXPECTED_WHITELIST = "103.21.244.0/22,103.22.200.0/22,2400:cb00::/32";

    @Autowired
    KubernetesClient client;

    @MockitoBean
    CloudflareIpsService cloudflareIpsService;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("kubernetes.disable.autoConfig", "true");
    }

    @BeforeEach
    void setUp() {
        when(cloudflareIpsService.getCachedCloudflareIps()).thenReturn(TEST_IPS);
        ensureNamespace();
    }

    @Test
    void whenAnnotationIsTrue_whitelistAnnotationIsSet() {
        Ingress ingress = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("my-app")
                        .withNamespace(NAMESPACE)
                        .withAnnotations(Map.of("cloudflare-ingress-guard.inias.eu/enabled", "true"))
                        .build())
                .build();
        client.resource(ingress).create();

        await().untilAsserted(() -> {
            Ingress updated = client.resources(Ingress.class)
                    .inNamespace(NAMESPACE)
                    .withName("my-app")
                    .get();
            assertThat(updated).isNotNull();
            assertThat(updated.getMetadata().getAnnotations())
                    .containsEntry("nginx.ingress.kubernetes.io/whitelist-source-range", EXPECTED_WHITELIST);
        });
    }

    @Test
    void whenAnnotationIsFalse_whitelistAnnotationIsRemoved() {
        Ingress ingress = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("my-app-disabled")
                        .withNamespace(NAMESPACE)
                        .withAnnotations(Map.of(
                                "cloudflare-ingress-guard.inias.eu/enabled", "false",
                                "nginx.ingress.kubernetes.io/whitelist-source-range", EXPECTED_WHITELIST
                        ))
                        .build())
                .build();
        client.resource(ingress).create();

        await().untilAsserted(() -> {
            Ingress updated = client.resources(Ingress.class)
                    .inNamespace(NAMESPACE)
                    .withName("my-app-disabled")
                    .get();
            assertThat(updated).isNotNull();
            assertThat(updated.getMetadata().getAnnotations())
                    .doesNotContainKey("nginx.ingress.kubernetes.io/whitelist-source-range");
        });
    }

    @Test
    void whenAnnotationIsAbsent_whitelistAnnotationIsNotSet() {
        Ingress ingress = new IngressBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("my-app-no-annotation")
                        .withNamespace(NAMESPACE)
                        .build())
                .build();
        client.resource(ingress).create();

        // Give the reconciler time to process; it should not modify the ingress
        await().during(ofSeconds(2)).untilAsserted(() -> {
            Ingress fetched = client.resources(Ingress.class)
                    .inNamespace(NAMESPACE)
                    .withName("my-app-no-annotation")
                    .get();
            assertThat(fetched).isNotNull();
            Map<String, String> annotations = fetched.getMetadata().getAnnotations();
            assertThat(annotations == null || !annotations.containsKey(
                    "nginx.ingress.kubernetes.io/whitelist-source-range")).isTrue();
        });
    }

    private void ensureNamespace() {
        if (client.namespaces().withName(NAMESPACE).get() == null) {
            client.namespaces().resource(
                    new io.fabric8.kubernetes.api.model.NamespaceBuilder()
                            .withMetadata(new ObjectMetaBuilder().withName(NAMESPACE).build())
                            .build()
            ).create();
        }
    }
}

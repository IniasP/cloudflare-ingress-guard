package eu.inias.cloudflareingressguard;

import eu.inias.cloudflareingressguard.model.CloudflareWhitelistFilter;
import eu.inias.cloudflareingressguard.model.CloudflareWhitelistFilterSpec;
import eu.inias.cloudflareingressguard.model.SnippetsFilter;
import eu.inias.cloudflareingressguard.services.CloudflareIpsService;
import eu.inias.cloudflareingressguard.testconfig.OperatorTestConfiguration;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableMockOperator(crdPaths = {
        "classpath:META-INF/fabric8/snippetsfilters.gateway.nginx.org-v1.yml",
        "classpath:META-INF/fabric8/cloudflarewhitelistfilters.cloudflareingressguard.inias.eu-v1.yml"
})
@Import(OperatorTestConfiguration.class)
class CloudflareWhitelistFilterReconcilerIntegrationTest {

    static final String NAMESPACE = "test";
    static final List<String> TEST_IPS = List.of("103.21.244.0/22", "103.22.200.0/22", "2400:cb00::/32");

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
    void whenCreated_snippetsFilterIsCreatedWithSameName() {
        client.resource(buildFilter("cloudflare-ips", null)).create();

        await().untilAsserted(() -> {
            SnippetsFilter sf = client.resources(SnippetsFilter.class)
                    .inNamespace(NAMESPACE)
                    .withName("cloudflare-ips")
                    .get();
            assertThat(sf).isNotNull();
            assertThat(sf.getSpec().getSnippets()).hasSize(1);
            String value = sf.getSpec().getSnippets().getFirst().getValue();
            assertThat(value).contains("allow 103.21.244.0/22;");
            assertThat(value).contains("allow 103.22.200.0/22;");
            assertThat(value).contains("allow 2400:cb00::/32;");
            assertThat(value).endsWith("deny all;");
            assertThat(sf.getSpec().getSnippets().getFirst().getContext())
                    .isEqualTo("http.server.location");
        });
    }

    @Test
    void whenSpecSnippetsFilterNameIsSet_snippetsFilterUsesCustomName() {
        client.resource(buildFilter("my-filter", "custom-name")).create();

        await().untilAsserted(() -> {
            SnippetsFilter sf = client.resources(SnippetsFilter.class)
                    .inNamespace(NAMESPACE)
                    .withName("custom-name")
                    .get();
            assertThat(sf).isNotNull();
            assertThat(sf.getSpec().getSnippets().getFirst().getValue())
                    .contains("allow 103.21.244.0/22;");
        });

        // The default-named SnippetsFilter must NOT exist
        assertThat(client.resources(SnippetsFilter.class)
                .inNamespace(NAMESPACE)
                .withName("my-filter")
                .get()).isNull();
    }

    @Test
    void snippetsFilterHasOwnerReferenceToCloudflareWhitelistFilter() {
        client.resource(buildFilter("owner-ref-test", null)).create();

        await().untilAsserted(() -> {
            SnippetsFilter sf = client.resources(SnippetsFilter.class)
                    .inNamespace(NAMESPACE)
                    .withName("owner-ref-test")
                    .get();
            assertThat(sf).isNotNull();
            assertThat(sf.getMetadata().getOwnerReferences()).hasSize(1);
            assertThat(sf.getMetadata().getOwnerReferences().getFirst().getName())
                    .isEqualTo("owner-ref-test");
            assertThat(sf.getMetadata().getOwnerReferences().getFirst().getKind())
                    .isEqualTo("CloudflareWhitelistFilter");
        });
    }

    @Test
    void statusIsUpdatedWithSnippetsFilterName() {
        client.resource(buildFilter("status-test", null)).create();

        await().untilAsserted(() -> {
            CloudflareWhitelistFilter updated = client.resources(CloudflareWhitelistFilter.class)
                    .inNamespace(NAMESPACE)
                    .withName("status-test")
                    .get();
            assertThat(updated).isNotNull();
            assertThat(updated.getStatus()).isNotNull();
            assertThat(updated.getStatus().getSnippetsFilterName()).isEqualTo("status-test");
        });
    }

    @Test
    void statusReflectsCustomSnippetsFilterName() {
        client.resource(buildFilter("status-custom-test", "my-custom-sf")).create();

        await().untilAsserted(() -> {
            CloudflareWhitelistFilter updated = client.resources(CloudflareWhitelistFilter.class)
                    .inNamespace(NAMESPACE)
                    .withName("status-custom-test")
                    .get();
            assertThat(updated).isNotNull();
            assertThat(updated.getStatus()).isNotNull();
            assertThat(updated.getStatus().getSnippetsFilterName()).isEqualTo("my-custom-sf");
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CloudflareWhitelistFilter buildFilter(String name, String snippetsFilterName) {
        CloudflareWhitelistFilter filter = new CloudflareWhitelistFilter();
        filter.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(NAMESPACE)
                .build());
        if (snippetsFilterName != null) {
            filter.setSpec(new CloudflareWhitelistFilterSpec(snippetsFilterName));
        }
        return filter;
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

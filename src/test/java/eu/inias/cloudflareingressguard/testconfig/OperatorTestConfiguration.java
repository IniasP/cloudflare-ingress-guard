package eu.inias.cloudflareingressguard.testconfig;

import io.javaoperatorsdk.operator.api.config.ConfigurationServiceOverrider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@TestConfiguration
public class OperatorTestConfiguration {

    /**
     * Disables Server-Side Apply (SSA) for patching primary resources.
     * The mock Kubernetes client does not support SSA, so this is required
     * for {@link io.javaoperatorsdk.operator.api.reconciler.UpdateControl#patchResource}
     * to work correctly in tests.
     */
    @Bean
    public Consumer<ConfigurationServiceOverrider> disableSsaConfigurationServiceOverrider() {
        return overrider -> overrider.withUseSSAToPatchPrimaryResource(false);
    }
}

// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.keyvault.spring;

import com.microsoft.azure.keyvault.spring.KeyVaultProperties.Property;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * Leverage {@link EnvironmentPostProcessor} to add Key Vault secrets as a property source.
 */
public class KeyVaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    public static final int DEFAULT_ORDER = ConfigFileApplicationListener.DEFAULT_ORDER + 1;
    private int order = DEFAULT_ORDER;

    /**
     * Post process the environment.
     *
     * <p>
     * Here we are going to process any key vault(s) and make them as available
     * PropertySource(s). Note this supports both the singular key vault setup,
     * as well as the multiple key vault setup.
     * </p>
     *
     * @param environment the environment.
     * @param application the application.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        final KeyVaultEnvironmentPostProcessorHelper helper
            = new KeyVaultEnvironmentPostProcessorHelper(environment);
        if (isKeyVaultEnabled(environment, "")) {
            helper.addKeyVaultPropertySource("");
        }
        if (hasMultipleKeyVaultsEnabled(environment)) {
            final String property = environment.getProperty(KeyVaultProperties.getPropertyName(Property.ORDER), "");
            final String[] keyVaultNames = property.split(",");
            for (int i = keyVaultNames.length - 1; i >= 0; i--) {
                final String normalizedName = keyVaultNames[i].trim();
                if (isKeyVaultEnabled(environment, normalizedName)) {
                    helper.addKeyVaultPropertySource(normalizedName);
                }
            }
        }
    }

    /**
     * Is the key vault enabled.
     *
     * @param environment    the environment.
     * @param normalizedName the normalized name used to differentiate between
     *                       multiple key vaults.
     * @return true if the key vault is enabled, false otherwise.
     */
    private boolean isKeyVaultEnabled(ConfigurableEnvironment environment, String normalizedName) {
        return environment.getProperty(
                KeyVaultProperties.getPropertyName(normalizedName, Property.ENABLED),
                Boolean.class,
                true)
            && environment.getProperty(KeyVaultProperties.getPropertyName(normalizedName, Property.URI)) != null
            && isKeyVaultClientAvailable();
    }

    /**
     * Determine whether or not multiple key vaults are enabled.
     *
     * @param environment the environment.
     * @return true if enabled, false otherwise.
     */
    private boolean hasMultipleKeyVaultsEnabled(ConfigurableEnvironment environment) {
        boolean result = false;
        if (environment.getProperty(KeyVaultProperties.getPropertyName(Property.ORDER)) != null) {
            result = true;
        }
        return result;
    }

    private boolean isKeyVaultClientAvailable() {
        return ClassUtils.isPresent("com.azure.security.keyvault.secrets.SecretClient",
            KeyVaultEnvironmentPostProcessor.class.getClassLoader());
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}

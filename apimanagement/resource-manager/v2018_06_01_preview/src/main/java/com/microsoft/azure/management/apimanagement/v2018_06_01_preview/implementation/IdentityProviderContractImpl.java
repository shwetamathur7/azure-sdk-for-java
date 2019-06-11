/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 *
 */

package com.microsoft.azure.management.apimanagement.v2018_06_01_preview.implementation;

import com.microsoft.azure.management.apimanagement.v2018_06_01_preview.IdentityProviderContract;
import com.microsoft.azure.arm.model.implementation.CreatableUpdatableImpl;
import rx.Observable;
import com.microsoft.azure.management.apimanagement.v2018_06_01_preview.IdentityProviderType;
import java.util.List;

class IdentityProviderContractImpl extends CreatableUpdatableImpl<IdentityProviderContract, IdentityProviderContractInner, IdentityProviderContractImpl> implements IdentityProviderContract, IdentityProviderContract.Definition, IdentityProviderContract.Update {
    private String resourceGroupName;
    private String serviceName;
    private IdentityProviderType identityProviderName;
    private String cifMatch;
    private String uifMatch;
    private final ApiManagementManager manager;

    IdentityProviderContractImpl(String name, ApiManagementManager manager) {
        super(name, new IdentityProviderContractInner());
        this.manager = manager;
        // Set resource name
        this.identityProviderName = IdentityProviderType.fromString(name);
        //
    }

    IdentityProviderContractImpl(IdentityProviderContractInner inner, ApiManagementManager manager) {
        super(inner.name(), inner);
        this.manager = manager;
        // Set resource name
        this.identityProviderName = IdentityProviderType.fromString(inner.name());
        // set resource ancestor and positional variables
        this.resourceGroupName = IdParsingUtils.getValueFromIdByName(inner.id(), "resourceGroups");
        this.serviceName = IdParsingUtils.getValueFromIdByName(inner.id(), "service");
        this.identityProviderName = IdentityProviderType.fromString(IdParsingUtils.getValueFromIdByName(inner.id(), "identityProviders"));
        // set other parameters for create and update
    }

    @Override
    public ApiManagementManager manager() {
        return this.manager;
    }

    @Override
    public Observable<IdentityProviderContract> createResourceAsync() {
        IdentityProvidersInner client = this.manager().inner().identityProviders();
        return client.createOrUpdateAsync(this.resourceGroupName, this.serviceName, this.identityProviderName, this.inner(), this.cifMatch)
            .map(innerToFluentMap(this));
    }

    @Override
    public Observable<IdentityProviderContract> updateResourceAsync() {
        IdentityProvidersInner client = this.manager().inner().identityProviders();
        return client.createOrUpdateAsync(this.resourceGroupName, this.serviceName, this.identityProviderName, this.inner(), this.uifMatch)
            .map(innerToFluentMap(this));
    }

    @Override
    protected Observable<IdentityProviderContractInner> getInnerAsync() {
        IdentityProvidersInner client = this.manager().inner().identityProviders();
        return null; // NOP getInnerAsync implementation as get is not supported
    }

    @Override
    public boolean isInCreateMode() {
        return this.inner().id() == null;
    }


    @Override
    public List<String> allowedTenants() {
        return this.inner().allowedTenants();
    }

    @Override
    public String authority() {
        return this.inner().authority();
    }

    @Override
    public String clientId() {
        return this.inner().clientId();
    }

    @Override
    public String clientSecret() {
        return this.inner().clientSecret();
    }

    @Override
    public String id() {
        return this.inner().id();
    }

    @Override
    public IdentityProviderType identityProviderContractType() {
        return this.inner().identityProviderContractType();
    }

    @Override
    public String name() {
        return this.inner().name();
    }

    @Override
    public String passwordResetPolicyName() {
        return this.inner().passwordResetPolicyName();
    }

    @Override
    public String profileEditingPolicyName() {
        return this.inner().profileEditingPolicyName();
    }

    @Override
    public String signinPolicyName() {
        return this.inner().signinPolicyName();
    }

    @Override
    public String signupPolicyName() {
        return this.inner().signupPolicyName();
    }

    @Override
    public String type() {
        return this.inner().type();
    }

    @Override
    public IdentityProviderContractImpl withResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
        return this;
    }

    @Override
    public IdentityProviderContractImpl withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    public IdentityProviderContractImpl withClientId(String clientId) {
        this.inner().withClientId(clientId);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withClientSecret(String clientSecret) {
        this.inner().withClientSecret(clientSecret);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withIfMatch(String ifMatch) {
        if (isInCreateMode()) {
            this.cifMatch = ifMatch;
        } else {
            this.uifMatch = ifMatch;
        }
        return this;
    }

    @Override
    public IdentityProviderContractImpl withAllowedTenants(List<String> allowedTenants) {
        this.inner().withAllowedTenants(allowedTenants);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withAuthority(String authority) {
        this.inner().withAuthority(authority);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withIdentityProviderContractType(IdentityProviderType identityProviderContractType) {
        this.inner().withIdentityProviderContractType(identityProviderContractType);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withPasswordResetPolicyName(String passwordResetPolicyName) {
        this.inner().withPasswordResetPolicyName(passwordResetPolicyName);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withProfileEditingPolicyName(String profileEditingPolicyName) {
        this.inner().withProfileEditingPolicyName(profileEditingPolicyName);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withSigninPolicyName(String signinPolicyName) {
        this.inner().withSigninPolicyName(signinPolicyName);
        return this;
    }

    @Override
    public IdentityProviderContractImpl withSignupPolicyName(String signupPolicyName) {
        this.inner().withSignupPolicyName(signupPolicyName);
        return this;
    }

}

/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 *
 */

package com.microsoft.azure.management.apimanagement.v2018_06_01_preview.implementation;

import com.microsoft.azure.arm.model.implementation.WrapperImpl;
import com.microsoft.azure.management.apimanagement.v2018_06_01_preview.ApiIssueAttachments;
import rx.Completable;
import rx.Observable;
import rx.functions.Func1;
import com.microsoft.azure.Page;
import com.microsoft.azure.management.apimanagement.v2018_06_01_preview.IssueAttachmentContract;

class ApiIssueAttachmentsImpl extends WrapperImpl<ApiIssueAttachmentsInner> implements ApiIssueAttachments {
    private final ApiManagementManager manager;

    ApiIssueAttachmentsImpl(ApiManagementManager manager) {
        super(manager.inner().apiIssueAttachments());
        this.manager = manager;
    }

    public ApiManagementManager manager() {
        return this.manager;
    }

    @Override
    public IssueAttachmentContractImpl define(String name) {
        return wrapModel(name);
    }

    private IssueAttachmentContractImpl wrapModel(IssueAttachmentContractInner inner) {
        return  new IssueAttachmentContractImpl(inner, manager());
    }

    private IssueAttachmentContractImpl wrapModel(String name) {
        return new IssueAttachmentContractImpl(name, this.manager());
    }

    @Override
    public Completable getEntityTagAsync(String resourceGroupName, String serviceName, String apiId, String issueId, String attachmentId) {
        ApiIssueAttachmentsInner client = this.inner();
        return client.getEntityTagAsync(resourceGroupName, serviceName, apiId, issueId, attachmentId).toCompletable();
    }

    @Override
    public Observable<IssueAttachmentContract> listByServiceAsync(final String resourceGroupName, final String serviceName, final String apiId, final String issueId) {
        ApiIssueAttachmentsInner client = this.inner();
        return client.listByServiceAsync(resourceGroupName, serviceName, apiId, issueId)
        .flatMapIterable(new Func1<Page<IssueAttachmentContractInner>, Iterable<IssueAttachmentContractInner>>() {
            @Override
            public Iterable<IssueAttachmentContractInner> call(Page<IssueAttachmentContractInner> page) {
                return page.items();
            }
        })
        .map(new Func1<IssueAttachmentContractInner, IssueAttachmentContract>() {
            @Override
            public IssueAttachmentContract call(IssueAttachmentContractInner inner) {
                return wrapModel(inner);
            }
        });
    }

    @Override
    public Observable<IssueAttachmentContract> getAsync(String resourceGroupName, String serviceName, String apiId, String issueId, String attachmentId) {
        ApiIssueAttachmentsInner client = this.inner();
        return client.getAsync(resourceGroupName, serviceName, apiId, issueId, attachmentId)
        .map(new Func1<IssueAttachmentContractInner, IssueAttachmentContract>() {
            @Override
            public IssueAttachmentContract call(IssueAttachmentContractInner inner) {
                return wrapModel(inner);
            }
       });
    }

    @Override
    public Completable deleteAsync(String resourceGroupName, String serviceName, String apiId, String issueId, String attachmentId, String ifMatch) {
        ApiIssueAttachmentsInner client = this.inner();
        return client.deleteAsync(resourceGroupName, serviceName, apiId, issueId, attachmentId, ifMatch).toCompletable();
    }

}

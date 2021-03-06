/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.eventgrid.v2020_04_01_preview;

import com.microsoft.azure.arm.collection.SupportsCreating;
import rx.Completable;
import rx.Observable;
import com.microsoft.azure.management.eventgrid.v2020_04_01_preview.implementation.PartnerTopicEventSubscriptionsInner;
import com.microsoft.azure.arm.model.HasInner;

/**
 * Type representing PartnerTopicEventSubscriptions.
 */
public interface PartnerTopicEventSubscriptions extends SupportsCreating<PartnerTopicEventSubscription.DefinitionStages.Blank>, HasInner<PartnerTopicEventSubscriptionsInner> {
    /**
     * Get full URL of an event subscription of a partner topic.
     * Get the full endpoint URL for an event subscription of a partner topic.
     *
     * @param resourceGroupName The name of the resource group within the user's subscription.
     * @param partnerTopicName Name of the partner topic.
     * @param eventSubscriptionName Name of the event subscription to be created. Event subscription names must be between 3 and 100 characters in length and use alphanumeric letters only.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    Observable<EventSubscriptionFullUrl> getFullUrlAsync(String resourceGroupName, String partnerTopicName, String eventSubscriptionName);

    /**
     * Get an event subscription of a partner topic.
     * Get an event subscription of a partner topic.
     *
     * @param resourceGroupName The name of the resource group within the user's subscription.
     * @param partnerTopicName Name of the partner topic.
     * @param eventSubscriptionName Name of the event subscription to be found. Event subscription names must be between 3 and 100 characters in length and use alphanumeric letters only.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    Observable<PartnerTopicEventSubscription> getAsync(String resourceGroupName, String partnerTopicName, String eventSubscriptionName);

    /**
     * List event subscriptions of a partner topic.
     * List event subscriptions that belong to a specific partner topic.
     *
     * @param resourceGroupName The name of the resource group within the user's subscription.
     * @param partnerTopicName Name of the partner topic.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    Observable<PartnerTopicEventSubscription> listByPartnerTopicAsync(final String resourceGroupName, final String partnerTopicName);

    /**
     * Delete an event subscription of a partner topic.
     * Delete an event subscription of a partner topic.
     *
     * @param resourceGroupName The name of the resource group within the user's subscription.
     * @param partnerTopicName Name of the partner topic.
     * @param eventSubscriptionName Name of the event subscription to be created. Event subscription names must be between 3 and 100 characters in length and use alphanumeric letters only.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    Completable deleteAsync(String resourceGroupName, String partnerTopicName, String eventSubscriptionName);

}

// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator

package com.microsoft.azure.keyvault.models;

import com.microsoft.rest.Base64Url;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The certificate restore parameters.
 */
public class CertificateRestoreParameters {
    /**
     * The backup blob associated with a certificate bundle.
     */
    @JsonProperty(value = "value", required = true)
    private Base64Url certificateBundleBackup;

    /**
     * Get the certificateBundleBackup value.
     *
     * @return the certificateBundleBackup value
     */
    public byte[] certificateBundleBackup() {
        if (this.certificateBundleBackup == null) {
            return null;
        }
        return this.certificateBundleBackup.decodedBytes();
    }

    /**
     * Set the certificateBundleBackup value.
     *
     * @param certificateBundleBackup the certificateBundleBackup value to set
     * @return the CertificateRestoreParameters object itself.
     */
    public CertificateRestoreParameters withCertificateBundleBackup(byte[] certificateBundleBackup) {
        if (certificateBundleBackup == null) {
            this.certificateBundleBackup = null;
        } else {
            this.certificateBundleBackup = Base64Url.encode(certificateBundleBackup);
        }
        return this;
    }

}
/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.media.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.internal.MediaServiceDataHolder;
import org.wso2.carbon.identity.media.core.model.MediaInformation;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.core.util.StorageSystemUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Controller class which invokes specific type of storage type implementation classes using factory pattern.
 */
public class StorageSystemManager {

    private static final Log LOGGER = LogFactory.getLog(StorageSystemManager.class);

    /**
     * Method which store an uploaded file to underlying storage system.
     *
     * @param inputStream   The input stream of the uploaded file.
     * @param mediaMetadata The metadata object associated with the uploaded file.
     * @param tenantDomain  The tenant domain of the service call.
     * @return unique id related to the uploaded resource.
     * @throws StorageSystemException Exception related to file upload.
     */
    public String addFile(List<InputStream> inputStream, MediaMetadata mediaMetadata, String tenantDomain)
            throws StorageSystemException {

        if (StringUtils.isNotBlank(tenantDomain)) {
            StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
            if (storageSystemFactory != null) {
                String uuid = StorageSystemUtil.calculateUUID();
                return storageSystemFactory.getInstance().addMedia(inputStream, mediaMetadata, uuid, tenantDomain);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null. Returning empty string.");
        }
        return "";

    }

    /**
     * Method which retrieves stored contents.
     *
     * @param id           The unique id related to the requesting resource.
     * @param tenantDomain The tenant domain of the service call.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @return requested file.
     * @throws StorageSystemException Exception related to retrieving the media.
     */
    public DataContent readContent(String id, String tenantDomain, String type) throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Download media for tenant domain %s.", tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().getFile(id, tenantDomain, type);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null hence returning null.");
        }
        return null;
    }

    /**
     * Security evaluation for downloading public resource.
     *
     * @param id                  The unique id related to the requesting resource.
     * @param type                The high level content-type of the resource (if media content-type is image/png then
     *                            type would be image).
     * @param tenantDomain        The tenant domain of the service call.
     * @return true if access to the resource is permitted.
     * @throws StorageSystemException Exception related to security evaluation during file download.
     */
    public boolean evaluateDownloadSecurityForPublicMedia(String id, String type, String tenantDomain)
            throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate security for media of type: %s, unique id: %s and tenant domain %s.",
                    id, type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().evaluateDownloadSecurityForPublicMedia(id, type, tenantDomain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null hence returning security evaluation result as false.");
        }
        return false;
    }

    /**
     * Security evaluation for downloading protected resource.
     *
     * @param id                  The unique id related to the requesting resource.
     * @param type                The high level content-type of the resource (if media content-type is image/png then
     *                            type would be image).
     * @param tenantDomain        The tenant domain of the service call.
     * @return true if access to the resource is permitted.
     * @throws StorageSystemException Exception related to security evaluation during file download.
     */
    public boolean evaluateDownloadSecurityForProtectedMedia(String id, String type, String tenantDomain)
            throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate download security for media of type: %s, unique id: %s and tenant " +
                            "domain %s.", id, type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().evaluateDownloadSecurityForProtectedMedia(id, type, tenantDomain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null hence returning security evaluation result as false.");
        }
        return false;
    }

    /**
     * Security evaluation for media management by an end-user.
     *
     * @param id                  The unique id of the media.
     * @param type                The high level content-type of the media (if media content-type is image/png then
     *                            type would be image).
     * @param tenantDomain        The tenant domain of the service call.
     * @return true if media management is permitted.
     * @throws StorageSystemException Exception related to security evaluation.
     */
    public boolean evaluateMediaManagementSecurityForEndUser(String id, String type, String tenantDomain)
            throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate media management security for media of type: %s, unique id: %s and " +
                    "tenant domain %s.", id, type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().evaluateMediaManagementSecurityForEndUser(id, type, tenantDomain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null hence returning security evaluation result as false.");
        }
        return false;
    }

    /**
     * Retrieve media information for the requested media.
     *
     * @param id           The unique id of the requested media.
     * @param type         The high level content-type of the media (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @return MediaInformation The media information.
     * @throws StorageSystemException Exception related to retrieving media information.
     */
    public MediaInformation retrieveMediaInformation(String id, String type, String tenantDomain)
            throws StorageSystemException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Retrieve information for media of type: %s, unique id: %s and tenant " +
                        "domain %s.", id, type, tenantDomain));
            }
            return storageSystemFactory.getInstance().getMediaInformation(id, type, tenantDomain);
        }
        return null;
    }

    /**
     * Method which deletes a stored file.
     *
     * @param id           The unique id of the requested resource.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @throws StorageSystemException Exception related to file deletion.
     * @return true if media is deleted successfully.
     */
    public boolean isMediaDeleted(String id, String type, String tenantDomain) throws StorageSystemException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Delete media of type: %s in tenant domain: %s.", type, tenantDomain));
            }
            return storageSystemFactory.getInstance().isMediaDeleted(id, type, tenantDomain);
        }
        return false;
    }

    /**
     * A method to do any transformation to the inputstream.
     *
     * @param id           unique id related to the requesting resource. (This id consists of uuid, a unique hash
     *                     value and a timestamp.)
     * @param type         Type of image (could be i,a, or u) i stands for idp,a stands for app, u stands for user
     * @param tenantDomain tenantdomain of the service call.
     * @param inputStream  inputstream of the file.
     * @return transformed inputstream.
     * @throws StorageSystemException
     */
    public InputStream transform(String id, String type, String tenantDomain, InputStream inputStream)
            throws StorageSystemException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Delete image for category %s and tenant domain %s.", type, tenantDomain));
            }
            return storageSystemFactory.getInstance().transform(id, type, tenantDomain, inputStream);
        }
        return new ByteArrayInputStream(new byte[0]);
    }

    private String readStorageTypeFromConfig() {

        String contentStoreType = IdentityUtil.getProperty("ContentStore.Type");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("The configured content store type is %s.", contentStoreType));
        }
        if (StringUtils.isEmpty(contentStoreType)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ContentStore.Type is not configured in identity.xml. Proceeding with the default value.");
            }
            contentStoreType = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
        }
        return contentStoreType;
    }

    private StorageSystemFactory getStorageSystemFactory(String storageType) {

        return MediaServiceDataHolder.getInstance().getStorageSystemFactories().get(storageType);

    }

}

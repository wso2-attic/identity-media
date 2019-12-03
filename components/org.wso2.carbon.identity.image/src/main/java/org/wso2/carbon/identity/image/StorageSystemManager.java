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
package org.wso2.carbon.identity.image;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.image.exception.StorageSystemException;
import org.wso2.carbon.identity.image.internal.ImageServiceDataHolder;
import org.wso2.carbon.identity.image.util.StorageSystemUtil;

import java.io.InputStream;

/**
 * Controller class which invokes specific type of storage type implementation classes using factory pattern.
 */
public class StorageSystemManager {

    private static final Log LOGGER = LogFactory.getLog(StorageSystemManager.class);

    /**
     * Method which store an uploaded file to underlying storage system.
     *
     * @param inputStream  the inputstream of the uploaded file
     * @param type         whether, file is of type, idp/sp/user
     * @param tenantDomain tenantdomain of the service call.
     * @return unique id related to the uploaded resource. unique id is calculated by concatenating uuid,a unique
     * hash value and a timestamp value.
     * @throws StorageSystemException Exception related to file upload
     */
    public String addFile(InputStream inputStream, String type, String tenantDomain) throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Upload image for category %s and tenant domain %s.", type, tenantDomain));
        }
        String uuid = StorageSystemUtil.calculateUUID();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("The unique id of the uploaded images is %s.", uuid));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().addFile(inputStream, type, uuid, tenantDomain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null. Returning empty string.");
        }
        return "";

    }

    /**
     * Method which retrieves a stored file.
     *
     * @param id           unique id related to the requesting resource. (This id consists of uuid, a unique hash value
     *                     and a timestamp.)
     * @param type         Type of image (could be i,a, or u) i stands for idp,a stands for app, u stands for user
     * @param tenantDomain tenantdomain of the service call.
     * @return inputstream of the file.
     * @throws StorageSystemException Exception related to retrieving the image
     */
    public byte[] getFile(String id, String type, String tenantDomain) throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Download image for category %s and tenant domain %s.", type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            return storageSystemFactory.getInstance().getFile(id, type, tenantDomain);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StorageSystemFactory object is null. Returning empty byte array.");
        }
        return new byte[0];
    }

    /**
     * Method which deletes a stored file.
     *
     * @param id           unique id related to the requesting resource. (This id consists of uuid, a unique hash value
     *                     and a timestamp.)
     * @param type         Type of image (could be i,a, or u) i stands for idp,a stands for app, u stands for user
     * @param tenantDomain tenantdomain of the service call.
     * @throws StorageSystemException Exception related to file deletion.
     */
    public void deleteFile(String id, String type, String tenantDomain) throws StorageSystemException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(readStorageTypeFromConfig());
        if (storageSystemFactory != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Delete image for category %s and tenant domain %s.", type, tenantDomain));
            }
            storageSystemFactory.getInstance().deleteFile(id, type, tenantDomain);
        }

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
            contentStoreType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        }
        return contentStoreType;
    }

    private StorageSystemFactory getStorageSystemFactory(String storageType) {

        return ImageServiceDataHolder.getInstance().getStorageSystemFactories().get(storageType);

    }

}

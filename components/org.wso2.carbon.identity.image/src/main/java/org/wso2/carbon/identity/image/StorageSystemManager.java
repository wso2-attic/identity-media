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
import org.apache.log4j.Logger;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.image.exception.StorageSystemException;
import org.wso2.carbon.identity.image.internal.ImageServiceDataHolder;
import org.wso2.carbon.identity.image.util.StorageSystemUtil;

import java.io.InputStream;

/**
 * Controller class which invokes specific type of storage type implementation classes using factory pattern.
 */
public class StorageSystemManager {

    private static final Logger LOGGER = Logger.getLogger(StorageSystemManager.class);

    /**
     * Method which store an uploaded file to underlying storage system.
     *
     * @param inputStream the inputstream of the uploaded file
     * @param type        whether, file is of type, idp/sp/user
     * @return unique id related to the uploaded resource. unique id is calculated by concatenating uuid,a unique
     * hash value and a timestamp value.
     */
    public String addFile(InputStream inputStream, String type) throws StorageSystemException {

        String storageType = readStorageTypeFromConfig();
        String uuid = new StorageSystemUtil().calculateUUID();
        return getStorageSystemFactory(storageType).getInstance().addFile(inputStream, type, uuid);
    }

    /**
     * Method which retrieves a stored file.
     *
     * @param id   unique id related to the requesting resource. (This id consists of uuid, a unique hash value and a
     *             timestamp.)
     * @param type Type of image (could be i,a, or u) i stands for idp,a stands for app, u stands for user
     * @return inputstream of the file.
     */
    public InputStream getFile(String id, String type) throws StorageSystemException {

        String storageType = readStorageTypeFromConfig();
        return getStorageSystemFactory(storageType).getInstance().getFile(id, type);
    }

    public void deleteFile(String id, String type) throws StorageSystemException {

        String storageType = readStorageTypeFromConfig();
        getStorageSystemFactory(storageType).getInstance().deleteFile(id, type);

    }

    private String readStorageTypeFromConfig() {

        String contentStoreType = IdentityUtil.getProperty("ContentStore.Type");
        if (StringUtils.isEmpty(contentStoreType)) {
            contentStoreType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        }
        return contentStoreType;
    }

    private StorageSystemFactory getStorageSystemFactory(String storageType) {

        return ImageServiceDataHolder.getInstance().getStorageSystemFactories().get(storageType);

    }

}

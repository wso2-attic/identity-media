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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.image.internal.ImageServiceDataHolder;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * Controller class which invokes specific type of storage type implementation classes using factory pattern.
 */
public class StorageSystemManager {

    /**
     * Method which store an uploaded file to underlying storage system.
     *
     * @param inputStream the inputstream of the uploaded file
     * @param type        wether, file is of type, idp/sp/user
     * @return unique id related to the uploaded resource. unique id is calculated by concatening uuid,hash of uuid
     * and a timestamp value.
     */
    public String addFile(InputStream inputStream, String type) {

        String storageType = readStorageTypeFromConfig();
        String uuid = UUID.randomUUID().toString();
        String uuidHash = calculateUUIDHash(uuid);
        long timeStamp = calculateTimeStamp();
        getStorageSystemFactory(storageType).getInstance().addFile(inputStream, type, uuid, timeStamp);

        return uuid + uuidHash + timeStamp;

    }

    public void getFile() {

    }

    public void deleteFile() {

    }

    private String readStorageTypeFromConfig() {

        String contentStoreType = IdentityUtil.getProperty("ContentStore.Type");
        if (StringUtils.isEmpty(contentStoreType)) {
            contentStoreType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        }
        return contentStoreType;
    }

    private StorageSystemfactory getStorageSystemFactory(String storageType) {

        return ImageServiceDataHolder.getInstance().getStorageSystemFactories().get(storageType);

    }

    private String calculateUUIDHash(String uuid) {

        return DigestUtils.sha256Hex(uuid);
    }

    private long calculateTimeStamp() {

        return new Date().getTime();

    }
}

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

import org.wso2.carbon.identity.image.internal.ImageServiceDataHolder;

import java.io.InputStream;

public class StorageSystemManager {

    public void addFile(InputStream inputStream, String type) {

        //read the type from carbon.xml
        String type1= "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        StorageSystemfactory storageSystemfactory = ImageServiceDataHolder.getInstance().getStorageSystemFactories()
                .get(type1);
        storageSystemfactory.getInstance().addFile(inputStream, type);
    }

    public void getFIle() {

    }

    public void deleteFile() {

    }
}

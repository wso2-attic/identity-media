/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.image.jdbc;

import org.wso2.carbon.identity.image.StorageSystem;

import java.io.InputStream;

/**
 * This class implements @ to store images in to database.
 */
public class DatabaseBasedStorageSystemImpl implements StorageSystem {

    @Override
    public void addFile(InputStream inputStream, String type) {

    }

    @Override
    public void getFIle() {

    }

    @Override
    public void deleteFile() {

    }
}

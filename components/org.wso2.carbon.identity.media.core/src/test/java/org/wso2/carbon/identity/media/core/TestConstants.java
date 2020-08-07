/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.media.core;

class TestConstants {

    static final String MEDIA_UUID = "30d0325e-40bc-45f3-845e-f13dd130e963";
    static final String MEDIA_TYPE = "image";
    static final String INVALID_MEDIA_TYPE = "application";
    static final String INVALID_MEDIA_TYPE_PATH_PARAM = "application";
    static final String CONTENT_SUB_TYPE = "png";
    static final String INVALID_CONTENT_SUB_TYPE = "pdf";
    static final String TENANT_DOMAIN = "carbon.super";
    static final int TENANT_ID = -1234;
    static final String FILE_BASED_MEDIA_STORE = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
    static final String DATABASE_BASED_MEDIA_STORE =
            "org.wso2.carbon.identity.media.jdbc.DatabaseBasedStorageSystemImpl";
    static final String INCORRECT_MEDIA_STORE = "org.wso2.carbon.identity.media.incorrect.IncorrectStorageSystemImpl";
    static final String FILE_NAME = "profilepic.png";
    static final String FILE_CONTENT_TYPE = "image/png";
    static final String USER_ID = "de0f7994-eb83-4cd9-96db-52cb62a1feaf";

}

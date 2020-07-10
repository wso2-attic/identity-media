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
package org.wso2.carbon.identity.media.core.util;

/**
 * Class to hold constants.
 */
public class StorageSystemConstants {

    public static final String SYSTEM_PROPERTY_CARBON_HOME = "carbon.home";
    public static final String PRE_CREATED_MEDIA_FOLDER = "media";
    public static final String METADATA_FILE_SUFFIX = "_meta";
    public static final String METADATA_FILE_EXTENSION = ".json";
    public static final String MEDIA_NAME = "name";
    public static final String MEDIA_CONTENT_TYPE = "contentType";
    public static final String MEDIA_TAG = "tag";
    public static final String MEDIA_RESOURCE_OWNER_ID = "resourceOwnerId";
    public static final String MEDIA_SECURITY = "security";
    public static final String MEDIA_SECURITY_ALLOWED_ALL = "allowedAll";
    public static final String MEDIA_SECURITY_ALLOWED_USER_IDS = "allowedUserIds";
    public static final String PUBLIC_DOWNLOAD_ACCESS = "public";
    public static final String PROTECTED_DOWNLOAD_ACCESS = "content";
    static final String MEDIA_PROPERTIES_FILE = "META-INF/media.properties";
    static final String MEDIA_STORE_TYPE = "MediaStoreType";
    static final String ALLOWED_MAXIMUM_SIZE_IN_BYTES = "AllowedMaximumSizeInBytes";
    static final String ALLOWED_CONTENT_TYPES = "AllowedContentTypes";
    static final String ALLOWED_CONTENT_SUB_TYPES = ".AllowedContentSubTypes";
    static final String MEDIA_MOUNT_LOCATION = "MediaMountLocation";

    // Environment variables to override default values defined in media.properties file.
    public static final String CONFIGURABLE_MEDIA_MOUNT_LOCATION = "MEDIA_MOUNT_LOCATION";
    public static final String CONFIGURABLE_MAXIMUM_MEDIA_SIZE_IN_BYTES = "MEDIA_MAX_BYTE_SIZE";
    public static final String CONFIGURABLE_MEDIA_STORE_TYPE = "MEDIA_STORE_TYPE";
    public static final String CONFIGURABLE_MEDIA_CONTENT_TYPES = "MEDIA_CONTENT_TYPES";

}

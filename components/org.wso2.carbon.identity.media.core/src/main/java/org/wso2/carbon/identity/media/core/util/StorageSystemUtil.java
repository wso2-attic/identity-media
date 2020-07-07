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
package org.wso2.carbon.identity.media.core.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.ALLOWED_CONTENT_SUB_TYPES;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.ALLOWED_CONTENT_TYPES;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.DEFAULT_MEDIA_STORE_TYPE_VALUE;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_PROPERTIES_FILE;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_STORE_TYPE;

/**
 * Util class to provide commonly used methods within the component.
 */
public class StorageSystemUtil {

    private static final Log LOGGER = LogFactory.getLog(StorageSystemUtil.class);

    private static String mediaStorageType;
    private static HashMap<String, List<String>> contentTypes = new HashMap<>();

    public static String calculateUUID() {

        return UUID.randomUUID().toString();
    }

    public static String getMediaStoreType() {

        return mediaStorageType;
    }

    public static HashMap<String, List<String>> getContentTypes() {

        return contentTypes;
    }
    /**
     * Read media properties defined in media.properties file.
     *
     * @throws StorageSystemException Exception related to loading configured media properties from file.
     */
    public static void loadMediaProperties() throws StorageSystemException {

        Properties properties = new Properties();
        ClassLoader classLoader = StorageSystemUtil.class.getClassLoader();
        if (classLoader != null) {
            InputStream inputStream = classLoader.getResourceAsStream(MEDIA_PROPERTIES_FILE);
            if (inputStream == null) {
                throw new StorageSystemException("Error while loading media.properties file.");
            }
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                throw new StorageSystemException("Error while loading media.properties file.", e);
            }

            String mediaStoreTypeConfig = properties.getProperty(MEDIA_STORE_TYPE);
            if (StringUtils.isNotBlank(mediaStoreTypeConfig)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("The configured media store type is %s.", mediaStoreTypeConfig));
                }
                mediaStorageType = mediaStoreTypeConfig;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MediaStoreType is not configured in media.properties file. Hence proceeding" +
                            " with the default value: " + DEFAULT_MEDIA_STORE_TYPE_VALUE);
                }
                mediaStorageType = DEFAULT_MEDIA_STORE_TYPE_VALUE;
            }

            String[] allowedContentTypes = properties.getProperty(ALLOWED_CONTENT_TYPES).split(",");
            for (String contentType : allowedContentTypes) {
                contentTypes.put(contentType, Arrays.asList(properties.getProperty(contentType +
                        ALLOWED_CONTENT_SUB_TYPES).split(",")));
            }
        }
    }
}

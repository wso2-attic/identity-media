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
package org.wso2.carbon.identity.media.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.UUID;

/**
 * Util class to provide commonly used methods within the component.
 */
public class StorageSystemUtil {

    private static final Log LOGGER = LogFactory.getLog(StorageSystemUtil.class);

    public static String calculateUUIDHash(String uuid, String timeStamp) {

        return DigestUtils.sha256Hex(uuid + timeStamp);
    }

    public static String calculateUUID() {
        return UUID.randomUUID().toString();
    }
}

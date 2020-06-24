/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.media.core.model;

import java.util.List;

/**
 * This class represents security applied on the uploaded media file.
 */
public class FileSecurity {

    private boolean allowedAll;
    private List<String> allowedUsers;

    public FileSecurity(boolean allowedAll) {

        this.allowedAll = allowedAll;
    }

    public FileSecurity(boolean allowedAll, List<String> allowedUsers) {

        this.allowedAll = allowedAll;
        this.allowedUsers = allowedUsers;
    }

    public boolean isAllowedAll() {

        return allowedAll;
    }

    public void setAllowedAll(boolean allowedAll) {

        this.allowedAll = allowedAll;
    }

    public List<String> getAllowedUsers() {

        return allowedUsers;
    }

    public void setAllowedUsers(List<String> allowedUsers) {

        this.allowedUsers = allowedUsers;
    }
}

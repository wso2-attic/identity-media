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
 * This class represents metadata associated with the uploaded media file.
 */
public class MediaMetadata {

    private String fileName;
    private String fileTag;
    private String fileContentType;
    private String resourceOwnerId;
    private List<String> fileIdentifiers;
    private FileSecurity fileSecurity;

    public String getFileTag() {

        return fileTag;
    }

    public void setFileTag(String fileTag) {

        this.fileTag = fileTag;
    }

    public FileSecurity getFileSecurity() {

        return fileSecurity;
    }

    public void setFileSecurity(FileSecurity fileSecurity) {

        this.fileSecurity = fileSecurity;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public List<String> getFileIdentifiers() {

        return fileIdentifiers;
    }

    public void setFileIdentifiers(List<String> fileIdentifiers) {

        this.fileIdentifiers = fileIdentifiers;
    }

    public String getFileContentType() {

        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {

        this.fileContentType = fileContentType;
    }

    public String getResourceOwnerId() {

        return resourceOwnerId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {

        this.resourceOwnerId = resourceOwnerId;
    }
}

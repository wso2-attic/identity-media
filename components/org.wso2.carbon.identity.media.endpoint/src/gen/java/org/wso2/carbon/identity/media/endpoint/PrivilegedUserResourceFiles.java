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

package org.wso2.carbon.identity.media.endpoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserMetadata;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class PrivilegedUserResourceFiles  {
  
    private List<File> files = new ArrayList<>();

    private PrivilegedUserMetadata metadata;

    /**
    **/
    public PrivilegedUserResourceFiles files(List<File> files) {

        this.files = files;
        return this;
    }
    
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("files")
    @Valid
    @NotNull(message = "Property files cannot be null.")

    public List<File> getFiles() {
        return files;
    }
    public void setFiles(List<File> files) {
        this.files = files;
    }

    public PrivilegedUserResourceFiles addFilesItem(File filesItem) {
        this.files.add(filesItem);
        return this;
    }

        /**
    **/
    public PrivilegedUserResourceFiles metadata(PrivilegedUserMetadata metadata) {

        this.metadata = metadata;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("metadata")
    @Valid
    public PrivilegedUserMetadata getMetadata() {
        return metadata;
    }
    public void setMetadata(PrivilegedUserMetadata metadata) {
        this.metadata = metadata;
    }



    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivilegedUserResourceFiles privilegedUserResourceFiles = (PrivilegedUserResourceFiles) o;
        return Objects.equals(this.files, privilegedUserResourceFiles.files) &&
            Objects.equals(this.metadata, privilegedUserResourceFiles.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, metadata);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class PrivilegedUserResourceFiles {\n");
        
        sb.append("    files: ").append(toIndentedString(files)).append("\n");
        sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
    * Convert the given object to string with each line indented by 4 spaces
    * (except the first line).
    */
    private String toIndentedString(java.lang.Object o) {

        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n");
    }
}


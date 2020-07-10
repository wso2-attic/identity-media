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
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserSecurityAllOf;
import org.wso2.carbon.identity.media.endpoint.Security;
import javax.validation.constraints.*;


import io.swagger.annotations.*;
import java.util.Objects;
import javax.validation.Valid;
import javax.xml.bind.annotation.*;

public class PrivilegedUserSecurity  {
  
    private Boolean allowedAll;
    private List<String> allowedUserIds = null;


    /**
    * Defines whether the file is publicly available for access or has restricted access.
    **/
    public PrivilegedUserSecurity allowedAll(Boolean allowedAll) {

        this.allowedAll = allowedAll;
        return this;
    }
    
    @ApiModelProperty(example = "false", value = "Defines whether the file is publicly available for access or has restricted access.")
    @JsonProperty("allowedAll")
    @Valid
    public Boolean getAllowedAll() {
        return allowedAll;
    }
    public void setAllowedAll(Boolean allowedAll) {
        this.allowedAll = allowedAll;
    }

    /**
    * The ids of set of users entitled to access the file.
    **/
    public PrivilegedUserSecurity allowedUserIds(List<String> allowedUserIds) {

        this.allowedUserIds = allowedUserIds;
        return this;
    }
    
    @ApiModelProperty(example = "[\"de0f7994-eb83-4cd9-96db-52cb62a1feaf\",\"fcb1940b-d669-4c58-8292-e8bca0f606b4\"]", value = "The ids of set of users entitled to access the file.")
    @JsonProperty("allowedUserIds")
    @Valid
    public List<String> getAllowedUserIds() {
        return allowedUserIds;
    }
    public void setAllowedUserIds(List<String> allowedUserIds) {
        this.allowedUserIds = allowedUserIds;
    }

    public PrivilegedUserSecurity addAllowedUserIdsItem(String allowedUserIdsItem) {
        if (this.allowedUserIds == null) {
            this.allowedUserIds = new ArrayList<>();
        }
        this.allowedUserIds.add(allowedUserIdsItem);
        return this;
    }

    

    @Override
    public boolean equals(java.lang.Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivilegedUserSecurity privilegedUserSecurity = (PrivilegedUserSecurity) o;
        return Objects.equals(this.allowedAll, privilegedUserSecurity.allowedAll) &&
            Objects.equals(this.allowedUserIds, privilegedUserSecurity.allowedUserIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedAll, allowedUserIds);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class PrivilegedUserSecurity {\n");
        
        sb.append("    allowedAll: ").append(toIndentedString(allowedAll)).append("\n");
        sb.append("    allowedUserIds: ").append(toIndentedString(allowedUserIds)).append("\n");
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


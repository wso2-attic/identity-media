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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Location(s) of media and metadata.
 **/

import java.util.Objects;
import javax.validation.Valid;

@ApiModel(description = "Location(s) of media and metadata.")
public class PrivilegedUserMediaInformationResponse  {
  
    private List<String> links = null;

    private PrivilegedUserMediaInformationResponseMetadata metadata;

    /**
    **/
    public PrivilegedUserMediaInformationResponse links(List<String> links) {

        this.links = links;
        return this;
    }

    @ApiModelProperty(example = "[\"/t/carbon.super/api/identity/media/v1.0/content/image/6e41cb95-c3b3-4e6c-928a-acb1b88e991d\"]", value = "")
    @JsonProperty("links")
    @Valid
    public List<String> getLinks() {
        return links;
    }
    public void setLinks(List<String> links) {
        this.links = links;
    }

    public PrivilegedUserMediaInformationResponse addLinksItem(String linksItem) {
        if (this.links == null) {
            this.links = new ArrayList<>();
        }
        this.links.add(linksItem);
        return this;
    }

        /**
    **/
    public PrivilegedUserMediaInformationResponse metadata(PrivilegedUserMediaInformationResponseMetadata metadata) {

        this.metadata = metadata;
        return this;
    }
    
    @ApiModelProperty(value = "")
    @JsonProperty("metadata")
    @Valid
    public PrivilegedUserMediaInformationResponseMetadata getMetadata() {
        return metadata;
    }
    public void setMetadata(PrivilegedUserMediaInformationResponseMetadata metadata) {
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
        PrivilegedUserMediaInformationResponse privilegedUserMediaInformationResponse = (PrivilegedUserMediaInformationResponse) o;
        return Objects.equals(this.links, privilegedUserMediaInformationResponse.links) &&
            Objects.equals(this.metadata, privilegedUserMediaInformationResponse.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links, metadata);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class PrivilegedUserMediaInformationResponse {\n");
        
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
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


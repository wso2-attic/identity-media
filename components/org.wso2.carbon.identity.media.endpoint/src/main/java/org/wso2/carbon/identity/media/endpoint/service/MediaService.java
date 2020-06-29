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

package org.wso2.carbon.identity.media.endpoint.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.model.FileSecurity;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.endpoint.Metadata;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserMetadata;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserSecurity;
import org.wso2.carbon.identity.media.endpoint.Security;
import org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.ALLOWED_CONTENT_SUB_TYPES;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.ALLOWED_CONTENT_TYPES;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.MEDIA_PROPERTIES_FILE_NAME;
import static org.wso2.carbon.identity.media.endpoint.common.Util.getStorageSystemManager;
import static org.wso2.carbon.identity.media.endpoint.common.Util.getTenantDomainFromContext;
import static org.wso2.carbon.identity.media.endpoint.common.Util.getUsernameFromContext;
import static org.wso2.carbon.identity.media.endpoint.common.Util.handleException;

/**
 * Perform media service related operations.
 */
public class MediaService {

    private static final Log LOG = LogFactory.getLog(MediaService.class);

    /**
     * Upload media by a privileged user.
     *
     * @param filesInputStream       The list of files to be uploaded as input streams.
     * @param filesDetail            File details of the list of files to be uploaded.
     * @param privilegedUserMetadata Metadata associated with the file upload.
     * @return unique identifier of the uploaded media.
     */
    public String uploadPrivilegedUserMedia(List<InputStream> filesInputStream, List<Attachment> filesDetail,
                                            PrivilegedUserMetadata privilegedUserMetadata) {

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.setResourceOwner(getUsernameFromContext());
        mediaMetadata.setFileName(filesDetail.get(0).getContentDisposition().getFilename());
        mediaMetadata.setFileContentType(filesDetail.get(0).getContentType().toString());

        if (privilegedUserMetadata != null) {
            mediaMetadata.setFileTag(privilegedUserMetadata.getTag());
        }

        setSecurityForPrivilegedUserUploadedMedia(privilegedUserMetadata, mediaMetadata);

        return addFile(filesInputStream, mediaMetadata);
    }

    /**
     * Upload media by an end-user.
     *
     * @param filesInputStream The list of files to be uploaded as input streams.
     * @param filesDetail      File details of the list of files to be uploaded.
     * @param metadata         Metadata associated with the file upload.
     * @return unique identifier of the uploaded media.
     */
    public String uploadMedia(List<InputStream> filesInputStream, List<Attachment> filesDetail, Metadata metadata) {

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.setResourceOwner(getUsernameFromContext());
        mediaMetadata.setFileName(filesDetail.get(0).getContentDisposition().getFilename());
        mediaMetadata.setFileContentType(filesDetail.get(0).getContentType().toString());

        if (metadata != null) {
            mediaMetadata.setFileTag(metadata.getTag());
        }

        setSecurityForEndUserUploadedMedia(metadata, mediaMetadata);

        return addFile(filesInputStream, mediaMetadata);
    }

    private String addFile(List<InputStream> filesInputStream, MediaMetadata mediaMetadata) {

        StorageSystemManager storageSystemManager = getStorageSystemManager();
        String tenantDomain = getTenantDomainFromContext();
        try {
            String uuid = storageSystemManager.addFile(filesInputStream, mediaMetadata, tenantDomain);
            if (StringUtils.isNotBlank(uuid)) {
                return uuid;
            }
            throw handleException(Response.Status.INTERNAL_SERVER_ERROR,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA);
        } catch (StorageSystemException e) {
            MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                    ERROR_CODE_ERROR_UPLOADING_MEDIA;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status);
        }
    }

    private void setSecurityForEndUserUploadedMedia(Metadata metadata, MediaMetadata mediaMetadata) {

        Security securityMeta = null;
        if (metadata != null) {
            securityMeta = metadata.getSecurity();
        }
        FileSecurity fileSecurity;
        if (securityMeta != null && securityMeta.getAllowedAll()) {
            fileSecurity = new FileSecurity(true);
        } else {
            ArrayList<String> users = new ArrayList<>();
            users.add(getUsernameFromContext());
            fileSecurity = new FileSecurity(false, users);
        }
        mediaMetadata.setFileSecurity(fileSecurity);
    }

    private void setSecurityForPrivilegedUserUploadedMedia(PrivilegedUserMetadata privilegedUserMetadata,
                                                           MediaMetadata mediaMetadata) {

        PrivilegedUserSecurity fileSecurityMeta = null;
        if (privilegedUserMetadata != null) {
            fileSecurityMeta = privilegedUserMetadata.getSecurity();
        }
        FileSecurity fileSecurity;
        if (fileSecurityMeta != null && fileSecurityMeta.getAllowedAll()) {
            fileSecurity = new FileSecurity(true);
        } else {
            List<String> allowedUsers = null;
            if (fileSecurityMeta != null) {
                allowedUsers = fileSecurityMeta.getAllowedUsers();
            }
            if (CollectionUtils.isNotEmpty(allowedUsers)) {
                fileSecurity = new FileSecurity(false, allowedUsers);
            } else {
                fileSecurity = new FileSecurity(false);
            }
        }
        mediaMetadata.setFileSecurity(fileSecurity);
    }

    /**
     * Validate if the uploaded media content type is a supported media type.
     *
     * @param mediaTypePathParam The media type available as a path parameter in the upload request.
     * @param mediaType          The content type of the uploaded media.
     */
    public void validateMediaType(String mediaTypePathParam, MediaType mediaType) {

        if (!StringUtils.equals(mediaTypePathParam, mediaType.getType())) {
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA_CONTENT_TYPE_MISMATCH,
                    mediaTypePathParam);
        }

        List<String> allowedContentTypes = loadAllowedContentTypes();
        if (CollectionUtils.isEmpty(allowedContentTypes) || !allowedContentTypes.contains(mediaTypePathParam)) {
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA_UNSUPPORTED_CONTENT_TYPE);
        }

        List<String> allowedContentSubTypes = loadAllowedContentSubTypes(mediaType.getType());
        if (CollectionUtils.isEmpty(allowedContentSubTypes) ||
                !allowedContentSubTypes.contains(mediaType.getSubtype())) {
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA_UNSUPPORTED_CONTENT_TYPE);
        }
    }

    /**
     * Read allowed content types defined in media.properties file.
     *
     * @return list of allowed content types.
     */
    private List<String> loadAllowedContentTypes() {

        Properties properties = new Properties();
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            if (classLoader != null) {
                properties.load(Objects.requireNonNull(classLoader.getResourceAsStream(MEDIA_PROPERTIES_FILE_NAME)));
                return Arrays.asList(properties.getProperty(ALLOWED_CONTENT_TYPES).split(","));
            }
        } catch (IOException e) {
            MediaServiceConstants.ErrorMessage errorMessage =
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_LOADING_ALLOWED_CONTENT_TYPES;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status, MEDIA_PROPERTIES_FILE_NAME);
        }
        return null;
    }

    /**
     * Read allowed sub types for a given content type in media.properties file.
     *
     * @return list of allowed sub content types for a given content type.
     */
    private List<String> loadAllowedContentSubTypes(String contentType) {

        Properties properties = new Properties();
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            if (classLoader != null) {
                properties.load(Objects.requireNonNull(classLoader.getResourceAsStream(MEDIA_PROPERTIES_FILE_NAME)));
                return Arrays.asList(properties.getProperty(contentType + ALLOWED_CONTENT_SUB_TYPES).split(","));
            }
        } catch (IOException e) {
            MediaServiceConstants.ErrorMessage errorMessage =
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_LOADING_ALLOWED_CONTENT_TYPES;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status, MEDIA_PROPERTIES_FILE_NAME);
        }
        return null;
    }

}

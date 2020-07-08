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

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.wso2.carbon.identity.media.core.DataContent;
import org.wso2.carbon.identity.media.core.FileContent;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.core.StreamContent;
import org.wso2.carbon.identity.media.core.exception.StorageSystemClientException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.model.FileSecurity;
import org.wso2.carbon.identity.media.core.model.MediaInformation;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.endpoint.Metadata;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserMetadata;
import org.wso2.carbon.identity.media.endpoint.PrivilegedUserSecurity;
import org.wso2.carbon.identity.media.endpoint.Security;
import org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    public void validateFileUploadMediaTypes(String mediaTypePathParam, MediaType mediaType) {

        String highLevelContentType = mediaType.getType();
        if (!StringUtils.equals(mediaTypePathParam, highLevelContentType)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Unable to upload the provided media due to a mismatch in the media type path" +
                                " parameter: %s and the actual media type of the media: %s", mediaTypePathParam,
                        highLevelContentType));
            }
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA_CONTENT_TYPE_MISMATCH,
                    mediaTypePathParam, highLevelContentType);
        }

        StorageSystemManager storageSystemManager = getStorageSystemManager();
        try {
            storageSystemManager.validateFileUploadMediaTypes(mediaTypePathParam, mediaType.getSubtype());
        } catch (StorageSystemClientException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to upload the provided media.", e);
            }
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UPLOADING_MEDIA_UNSUPPORTED_CONTENT_TYPE,
                    mediaType.toString());
        }
    }

    /**
     * Download requested media file.
     *
     * @param type       The high level content-type of the resource (if media content-type is image/png then
     *                   type would be image).
     * @param id         Unique identifier for the requested media.
     * @param identifier File identifier.
     * @return requested media file.
     */
    public Response downloadMediaFile(String type, String id, String identifier) {

        // Retrieving a sub-representation of a media is not supported during the first phase of the implementation.
        if (StringUtils.isNotBlank(identifier)) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        }

        validateAllowedMediaTypes(type);

        DataContent resource = downloadMedia(type, id);
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(86400);
        cacheControl.setPrivate(true);

        if (resource instanceof FileContent) {
            return Response.ok(((FileContent) resource).getFile()).header(HTTPConstants.HEADER_CONTENT_TYPE,
                    ((FileContent) resource).getResponseContentType()).cacheControl(cacheControl).build();
        } else if (resource instanceof StreamContent) {
            return Response.ok().entity(((StreamContent) resource).getInputStream()).header(
                    HTTPConstants.HEADER_CONTENT_TYPE, ((StreamContent) resource).getResponseContentType())
                    .cacheControl(cacheControl).build();
        }
        MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage
                .ERROR_CODE_ERROR_DOWNLOADING_MEDIA;
        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        throw handleException(status, errorMessage, id);
    }

    private DataContent downloadMedia(String type, String id) {

        StorageSystemManager storageSystemManager = getStorageSystemManager();
        String tenantDomain = getTenantDomainFromContext();
        try {
            DataContent dataContent = storageSystemManager.readContent(id, tenantDomain, type);
            if (dataContent == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Media download request can't be performed as media with id: %s of " +
                            "type: %s in tenant domain: %s is not existing.", id, type, tenantDomain));
                }
                throw handleException(Response.Status.NOT_FOUND,
                        MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_DOWNLOADING_MEDIA_FILE_NOT_FOUND, id);
            }
            return dataContent;
        } catch (StorageSystemException e) {
            MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                    ERROR_CODE_ERROR_DOWNLOADING_MEDIA;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status, id);
        }
    }

    /**
     * Validate if the media content type present as a path parameter in the request is a supported content type.
     *
     * @param mediaTypePathParam The media type available as a path parameter in the request.
     */
    private void validateAllowedMediaTypes(String mediaTypePathParam) {

        StorageSystemManager storageSystemManager = getStorageSystemManager();
        try {
            storageSystemManager.validateMediaTypePathParam(mediaTypePathParam);
        } catch (StorageSystemClientException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to perform requested media operation.", e);
            }
            throw handleException(Response.Status.FORBIDDEN,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_UNSUPPORTED_CONTENT_TYPE_PATH_PARAM);
        }
    }

    /**
     * Delete a media.
     *
     * @param type The high level content-type of the resource (if media content-type is image/png then
     *             type would be image.
     * @param id   Unique identifier for the requested media file to be deleted.
     */
    public void deleteMedia(String type, String id) {

        validateAllowedMediaTypes(type);
        StorageSystemManager storageSystemManager = getStorageSystemManager();
        String tenantDomain = getTenantDomainFromContext();
        try {
            storageSystemManager.deleteMedia(id, type, tenantDomain);
        } catch (StorageSystemClientException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Client exception while deleting media.", e);
            }
            throw handleException(Response.Status.NOT_FOUND,
                    MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_DELETING_MEDIA_FILE_NOT_FOUND, id, type);
        } catch (StorageSystemException e) {
            MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                    ERROR_CODE_ERROR_DELETING_MEDIA;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status, id);
        }
    }

    /**
     * Get media information for media requested.
     *
     * @param type The high level content-type of the resource (if media content-type is image/png then type would be
     *             image.
     * @param id   Unique identifier for the requested media.
     * @return MediaInformation The media information object.
     */
    public MediaInformation getMediaInformation(String type, String id) {

        validateAllowedMediaTypes(type);
        StorageSystemManager storageSystemManager = getStorageSystemManager();
        String tenantDomain = getTenantDomainFromContext();

        try {
            MediaInformation mediaInformation = storageSystemManager.retrieveMediaInformation(id, type, tenantDomain);
            if (mediaInformation == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Media information retrieval request can't be performed as media with " +
                            "id: %s of type: %s in tenant domain: %s is not existing.", id, type, tenantDomain));
                }
                throw handleException(Response.Status.NOT_FOUND,
                        MediaServiceConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_MEDIA_INFORMATION_FILE_NOT_FOUND,
                        id);
            }
            return mediaInformation;
        } catch (StorageSystemException e) {
            MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                    ERROR_CODE_ERROR_RETRIEVING_MEDIA_INFORMATION;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status, id);
        }
    }

}

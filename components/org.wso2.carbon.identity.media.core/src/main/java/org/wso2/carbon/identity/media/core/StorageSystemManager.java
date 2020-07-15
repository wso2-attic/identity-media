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
package org.wso2.carbon.identity.media.core;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.media.core.exception.StorageSystemClientException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemServerException;
import org.wso2.carbon.identity.media.core.internal.MediaServiceDataHolder;
import org.wso2.carbon.identity.media.core.model.MediaInformation;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.core.util.StorageSystemUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.CONFIGURABLE_MAXIMUM_MEDIA_SIZE_IN_BYTES;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.CONFIGURABLE_MEDIA_CONTENT_TYPES;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.CONFIGURABLE_MEDIA_STORE_TYPE;

/**
 * Controller class which invokes specific type of storage type implementation classes using factory pattern.
 */
public class StorageSystemManager {

    private static final Log LOGGER = LogFactory.getLog(StorageSystemManager.class);

    /**
     * Method which store an uploaded file to underlying storage system.
     *
     * @param inputStream   The input stream of the uploaded file.
     * @param mediaMetadata The metadata object associated with the uploaded file.
     * @param tenantDomain  The tenant domain of the service call.
     * @return unique id related to the uploaded resource.
     * @throws StorageSystemServerException The server exception related to file upload.
     */
    public String addFile(List<InputStream> inputStream, MediaMetadata mediaMetadata, String tenantDomain)
            throws StorageSystemServerException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        // id for the uploaded media is calculated by concatenating current date (in yyyyMMdd format) with uuid.
        String id = LocalDate.now().format(formatter) + "-" + StorageSystemUtil.calculateUUID();

        return storageSystemFactory.getInstance().addMedia(inputStream, mediaMetadata, id, tenantDomain);
    }

    /**
     * Method which retrieves stored contents.
     *
     * @param id           The unique id related to the requesting resource.
     * @param tenantDomain The tenant domain of the service call.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @return requested file.
     * @throws StorageSystemException Exception related to retrieving the media.
     */
    public DataContent readContent(String id, String tenantDomain, String type) throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Download media for tenant domain %s.", tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        return storageSystemFactory.getInstance().getFile(id, tenantDomain, type);
    }

    /**
     * Security evaluation for downloading public resource.
     *
     * @param id           The unique id related to the requesting resource.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @return true if access to the resource is permitted.
     * @throws StorageSystemServerException The server exception related to security evaluation during file download.
     */
    public boolean isDownloadAllowedForPublicMedia(String id, String type, String tenantDomain)
            throws StorageSystemServerException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate security for media of type: %s, unique id: %s and tenant domain %s.",
                    id, type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        return storageSystemFactory.getInstance().isDownloadAllowedForPublicMedia(id, type, tenantDomain);
    }

    /**
     * Security evaluation for downloading protected resource.
     *
     * @param mediaId      The unique id related to the requesting resource.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @param username     The username of the user.
     * @return true if access to the resource is permitted.
     * @throws StorageSystemServerException The server exception related to security evaluation during file download.
     */
    public boolean isDownloadAllowedForProtectedMedia(String mediaId, String type, String tenantDomain, String username)
            throws StorageSystemServerException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate download security for media of type: %s, unique id: %s and tenant " +
                            "domain %s.", type, mediaId, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        String userId = getUserIdFromUserName(username);
        return storageSystemFactory.getInstance().isDownloadAllowedForProtectedMedia(mediaId, type, tenantDomain,
                userId);
    }

    /**
     * Security evaluation for media management by an end-user.
     *
     * @param mediaId      The unique id of the media.
     * @param type         The high level content-type of the media (if media content-type is image/png then type would
     *                     be image).
     * @param tenantDomain The tenant domain of the service call.
     * @param username     The username of the user.
     * @return true if media management is permitted.
     * @throws StorageSystemServerException The server exception related to security evaluation.
     */
    public boolean isMediaManagementAllowedForEndUser(String mediaId, String type, String tenantDomain, String username)
            throws StorageSystemServerException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Evaluate media management security for media of type: %s, unique id: %s and " +
                    "tenant domain %s.", type, mediaId, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        String userId = getUserIdFromUserName(username);
        return storageSystemFactory.getInstance().isMediaManagementAllowedForEndUser(mediaId, type, tenantDomain,
                userId);
    }

    /**
     * Retrieve media information for the requested media.
     *
     * @param id           The unique id of the requested media.
     * @param type         The high level content-type of the media (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @return MediaInformation The media information.
     * @throws StorageSystemException Exception related to retrieving media information.
     */
    public MediaInformation retrieveMediaInformation(String id, String type, String tenantDomain)
            throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Retrieve information for media of type: %s, unique id: %s and tenant " +
                    "domain %s.", id, type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        return storageSystemFactory.getInstance().getMediaInformation(id, type, tenantDomain);
    }

    /**
     * Method which deletes a stored file.
     *
     * @param id           The unique id of the requested resource.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain The tenant domain of the service call.
     * @throws StorageSystemException Exception related to file deletion.
     */
    public void deleteMedia(String id, String type, String tenantDomain) throws StorageSystemException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Delete media of type: %s in tenant domain: %s.", type, tenantDomain));
        }
        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());
        storageSystemFactory.getInstance().deleteMedia(id, type, tenantDomain);
    }

    /**
     * A method to do any transformation to the inputstream.
     *
     * @param id           The unique id related to the requesting resource.
     * @param type         The high level content-type of the resource (if media content-type is image/png then
     *                     type would be image).
     * @param tenantDomain tenantdomain of the service call.
     * @param inputStream  inputstream of the file.
     * @return transformed inputstream.
     * @throws StorageSystemException
     */
    public InputStream transform(String id, String type, String tenantDomain, InputStream inputStream)
            throws StorageSystemException {

        StorageSystemFactory storageSystemFactory = getStorageSystemFactory(getMediaStoreType());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Transform media for category %s and tenant domain %s.", type, tenantDomain));
        }
        return storageSystemFactory.getInstance().transform(id, type, tenantDomain, inputStream);

    }

    /**
     * Validate if the uploaded media content type is a supported media type.
     *
     * @param mediaTypePathParam The media type available as a path parameter in the upload request.
     * @param contentSubType     The  subtype of the uploaded media (if media content-type is image/png then
     *                           subtype would be png).
     * @throws StorageSystemClientException Client exception related to validating media type of the media to be
     *                                      uploaded.
     */
    public void validateFileUploadMediaTypes(String mediaTypePathParam, String contentSubType)
            throws StorageSystemClientException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Started content type validation for media to be uploaded.");
        }

        String envForAllowedContentTypes = System.getenv(CONFIGURABLE_MEDIA_CONTENT_TYPES);
        HashMap<String, List<String>> allowedContentTypes = new HashMap<>();
        if (StringUtils.isNotBlank(envForAllowedContentTypes)) {
            String[] contentTypes = envForAllowedContentTypes.split(",");
            for (String contentType : contentTypes) {
                String environmentVariableForAllowedContentSubTypes =
                        System.getenv(String.format("MEDIA_%s_CONTENT_SUB_TYPES",
                                contentType.toUpperCase(Locale.ENGLISH)));
                if (StringUtils.isNotBlank(environmentVariableForAllowedContentSubTypes)) {
                    allowedContentTypes.put(contentType, Arrays.asList(environmentVariableForAllowedContentSubTypes
                            .split(",")));
                } else {
                    allowedContentTypes.put(contentType, null);
                }
            }
        } else {
            allowedContentTypes = StorageSystemUtil.getContentTypes();
        }

        if (MapUtils.isEmpty(allowedContentTypes) || !allowedContentTypes.keySet().contains(mediaTypePathParam)) {
            throw new StorageSystemClientException(String.format("Uploading media of content-type: %s/%s is not " +
                    "allowed.", mediaTypePathParam, contentSubType));
        }

        List<String> allowedContentSubTypes = allowedContentTypes.get(mediaTypePathParam);
        if (CollectionUtils.isEmpty(allowedContentSubTypes) || !allowedContentSubTypes.contains(contentSubType)) {
            throw new StorageSystemClientException(String.format("Uploading media of content-type: %s/%s is not " +
                    "allowed.", mediaTypePathParam, contentSubType));
        }
    }

    /**
     * Validate if the media content type path parameter in the request is a supported content type.
     *
     * @param mediaTypePathParam The media type available as a path parameter in the request.
     * @throws StorageSystemClientException Client exception related to validating media type path parameter.
     */
    public void validateMediaTypePathParam(String mediaTypePathParam) throws StorageSystemClientException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Started validating if the media content type path parameter in the request is a " +
                    "supported content type.");
        }

        String envForAllowedContentTypes = System.getenv(CONFIGURABLE_MEDIA_CONTENT_TYPES);
        Set<String> allowedContentTypes;
        if (StringUtils.isNotBlank(envForAllowedContentTypes)) {
            String[] contentTypes = envForAllowedContentTypes.split(",");
            allowedContentTypes = new HashSet<>(Arrays.asList(contentTypes));

        } else {
            allowedContentTypes = StorageSystemUtil.getContentTypes().keySet();
        }

        if (CollectionUtils.isEmpty(allowedContentTypes) || !allowedContentTypes.contains(mediaTypePathParam)) {
            throw new StorageSystemClientException(String.format("Unsupported file content type: %s available as a " +
                    "path parameter in the request.", mediaTypePathParam));
        }
    }

    /**
     * Retrieve the username of the user who is making the request to the media service.
     *
     * @param username The username.
     * @return user id.
     * @throws StorageSystemServerException The server exception related retrieving user ID from username.
     */
    public String getUserIdFromUserName(String username) throws StorageSystemServerException {

        try {
            RealmService realmService = MediaServiceDataHolder.getInstance().getRealmService();
            int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            UserRealm userRealm = realmService.getTenantUserRealm(tenantID);
            if (userRealm != null) {
                UserStoreManager userStoreManager = (UserStoreManager) userRealm.getUserStoreManager();
                String userIdFromUserName = ((AbstractUserStoreManager) userStoreManager)
                        .getUserIDFromUserName(username);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("The user id for the username: %s retrieved successfully.", username));
                }
                return userIdFromUserName;
            }
        } catch (UserStoreException e) {
            throw new StorageSystemServerException("Error occurred while retrieving the userstore manager to resolve" +
                    " id for the user: " + username, e);
        }
        return null;
    }

    /**
     * Validates if the file size of the media to be uploaded doesn't exceed the maximum allowed file size.
     *
     * @param inputStream The media as an input.
     * @throws StorageSystemException Exception related to validating media size.
     */
    public void validateMediaSize(InputStream inputStream) throws StorageSystemException {

        String envForMediaSize = System.getenv(CONFIGURABLE_MAXIMUM_MEDIA_SIZE_IN_BYTES);
        int allowedMaximumMediaSize;
        if (StringUtils.isNotBlank(envForMediaSize)) {
            allowedMaximumMediaSize = Integer.parseInt(envForMediaSize);
        } else {
            allowedMaximumMediaSize = StorageSystemUtil.getMediaMaximumSize();
        }

        try {
            byte[] mediaByteArray = IOUtils.toByteArray(inputStream);
            if (mediaByteArray.length > allowedMaximumMediaSize) {
                throw new StorageSystemClientException(String.format("The uploaded media size: %skb exceeds the " +
                                "maximum allowed file size: %skb", mediaByteArray.length / 1000.0,
                        allowedMaximumMediaSize / 1000.0));
            }
            inputStream.reset();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("The file size: %skb of the media to be uploaded doesn't exceed the " +
                                "maximum allowed file size: %skb. Hence proceeding with the upload.",
                        mediaByteArray.length / 1000.0, allowedMaximumMediaSize / 1000.0));
            }
        } catch (IOException e) {
            throw new StorageSystemServerException("Error occurred while calculating media size.", e);
        }
    }

    private String getMediaStoreType() {

        String envForMediaStoreType = System.getenv(CONFIGURABLE_MEDIA_STORE_TYPE);
        if (StringUtils.isNotBlank(envForMediaStoreType)) {
            return envForMediaStoreType;
        } else {
            return StorageSystemUtil.getMediaStoreType();
        }
    }

    private StorageSystemFactory getStorageSystemFactory(String storageType) throws StorageSystemServerException {

        StorageSystemFactory storageSystemFactory =
                MediaServiceDataHolder.getInstance().getStorageSystemFactories().get(storageType);
        if (storageSystemFactory == null) {
            throw new StorageSystemServerException(String.format("Unable to obtain StorageSystemFactory for " +
                    "configured media store type: %s", storageType));
        }
        return storageSystemFactory;
    }

}

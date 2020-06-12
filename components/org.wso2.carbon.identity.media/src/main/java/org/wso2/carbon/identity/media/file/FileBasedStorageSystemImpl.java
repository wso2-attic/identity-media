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
package org.wso2.carbon.identity.media.file;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.media.DataContent;
import org.wso2.carbon.identity.media.FileContentImpl;
import org.wso2.carbon.identity.media.StorageSystem;
import org.wso2.carbon.identity.media.exception.StorageSystemException;
import org.wso2.carbon.identity.media.model.FileSecurity;
import org.wso2.carbon.identity.media.model.MediaMetadata;
import org.wso2.carbon.identity.media.util.StorageSystemUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.wso2.carbon.identity.media.util.StorageSystemConstants.CONFIGURABLE_UPLOAD_LOCATION;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.DEFAULT;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.FILE_CREATION_TIME_ATTRIBUTE;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.FILE_SECURITY_ALLOWED_ALL;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.FILE_SECURITY_ALLOWED_SCOPES;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.FILE_SECURITY_ALLOWED_USERS;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.IDP;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.ID_SEPERATOR;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.MEDIA_STORE;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_CONTENT_TYPE;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_CREATED_TIME;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_EXTENSION;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_LAST_ACCESSED_TIME;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_NAME;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_SECURITY;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_SUFFIX;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.METADATA_FILE_TAG;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.SP;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.SYSTEM_PROPERTY_CARBON_HOME;
import static org.wso2.carbon.identity.media.util.StorageSystemConstants.USER;

/**
 * This is the implementation class to store, retrieve and delete media in the local file system.
 */
public class FileBasedStorageSystemImpl implements StorageSystem {

    private static final Log LOGGER = LogFactory.getLog(FileBasedStorageSystemFactory.class);

    @Override
    public String addFile(List<InputStream> inputStreams, MediaMetadata mediaMetadata, String uuid,
                          String tenantDomain) throws StorageSystemException {

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Uploading media file with unique id: %s and in tenant domain: %s", uuid,
                        tenantDomain));
            }
            return uploadMediaUsingChannels(inputStreams, mediaMetadata, uuid, tenantDomain);
        } catch (IOException e) {
            throw new StorageSystemException("Error while uploading media to file system.", e);
        }

    }

    @Override
    public DataContent getFile(String id, String type, String tenantDomain) throws StorageSystemException {

        String[] imageUniqueIdElements = retrieveImageUniqueIdElements(id);
        File file;
        try {
            file = getImageFile(imageUniqueIdElements, type, tenantDomain);
            FileContentImpl fileContent = new FileContentImpl(file);
            return fileContent;
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving the stored file of type %s.", type);
            throw new StorageSystemException(errorMsg, e);
        }

    }

    @Override
    public void deleteFile(String id, String type, String tenantDomain) throws StorageSystemException {

        String[] urlElements = retrieveImageUniqueIdElements(id);
        try {
            deleteImageFile(urlElements, type, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while deleting the stored file of type %s.", type);
            throw new StorageSystemException(errorMsg, e);
        }

    }

    @Override
    public InputStream transform(String id, String type, String tenantDomain, InputStream inputStream)
            throws StorageSystemException {

        return inputStream;
    }

    private String uploadMediaUsingChannels(List<InputStream> fileInputStreams, MediaMetadata mediaMetadata,
                                            String uuid, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String fileContentType = mediaMetadata.getFileContentType();
        String fileType = fileContentType.split("/")[0];
        String fileTag = mediaMetadata.getFileTag();
        String fileName = mediaMetadata.getFileName();
        FileSecurity fileSecurity = mediaMetadata.getFileSecurity();

        Path mediaStoragePath = createStorageDirectory(fileType, tenantId, uuid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Uploading media file to directory %s, for tenant id %d",
                    mediaStoragePath.toString(), tenantId));
        }
        if (mediaStoragePath != null) {
            Path targetLocation = mediaStoragePath.resolve(uuid);
            File file = targetLocation.toFile();
            // Currently, only single file upload is allowed.
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                 FileChannel fileChannel = fileOutputStream.getChannel();
                 ReadableByteChannel readableByteChannel = Channels.newChannel(fileInputStreams.get(0))) {
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                if (LOGGER.isDebugEnabled()) {
                    byte[] imageByteCount = IOUtils.toByteArray(fileInputStreams.get(0));
                    LOGGER.debug(String.format("Writing media data of size %d to a file named %s at location %s.",
                            imageByteCount.length, uuid, targetLocation.toString()));
                }
            }

            FileTime createdTime = (FileTime) Files.getAttribute(targetLocation, FILE_CREATION_TIME_ATTRIBUTE);
            String timeStampAsString = Long.toString(createdTime.toMillis());
            storeMediaMetadata(targetLocation, fileName, fileContentType, fileTag, fileSecurity, timeStampAsString);

            return uuid;
        }
        // TODO: 11/29/19 Add proper warnings for e.g Disk Full /File permission scenarios.

        return "";
    }

    private Path createStorageDirectory(String fileType, int tenantId, String uuid) throws IOException {

        Path fileStorageLocation = null;
        Path configurableRootFolder = null;
        String systemPropertyForRootFolder = System.getProperty(CONFIGURABLE_UPLOAD_LOCATION);
        if (systemPropertyForRootFolder != null) {
            configurableRootFolder = Paths.get(systemPropertyForRootFolder);
        }

        if (configurableRootFolder == null) {
            configurableRootFolder = Paths.get(System.getProperty(SYSTEM_PROPERTY_CARBON_HOME));
        }

        if (configurableRootFolder != null) {
            fileStorageLocation = configurableRootFolder.resolve(Paths.get(MEDIA_STORE + fileType));
        }

        Path mediaPath = null;

        if (fileStorageLocation != null) {
            mediaPath = Files.createDirectories(fileStorageLocation.resolve(String.valueOf(tenantId)));
        }

        return createUniqueDirectoryStructure(mediaPath, uuid);

    }

    private Path createUniqueDirectoryStructure(Path path, String uuid) throws IOException {

        String[] uuidSplit = uuid.split("-");
        Path uniquePath;
        if (path != null) {
            uniquePath = path;
            for (int i = 1; i <= uuidSplit.length; i++) {
                uniquePath = uniquePath.resolve(uuidSplit[uuidSplit.length - i]);
            }
            return Files.createDirectories(uniquePath);
        }
        return null;

    }

    /**
     * The GET url will be in the format of https://localhost:9443/t/carbon
     * .super/api/server/v1/images/{type}/uuid_unique-hash_timestamp.
     * This method will return the uuid,unique-hash and timestamp in an array after splitting using the seperator
     * (underscore)
     *
     * @param id url fragment defining the unique id of the resource
     * @return String array containing uuid,unique-hash and timestamp
     */
    private String[] retrieveImageUniqueIdElements(String id) {

        if (id == null) {
            return new String[0];
        }
        return id.split(ID_SEPERATOR);

    }

    private boolean validate(String[] urlElements, long createdTime) {

        return urlElements[2].equals(Long.toString(createdTime)) && urlElements[1]
                .equals(StorageSystemUtil.calculateUUIDHash(urlElements[0], urlElements[2]));

    }

    private File getImageFile(String[] urlElements, String type, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String imageCategoryType = getImageCategoryType(type);
        Path fileStorageLocation = createStorageDirectory(imageCategoryType, tenantId, urlElements[0]);
        if (fileStorageLocation != null) {
            String fileName = urlElements[0];
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            FileTime createdTime = (FileTime) Files.getAttribute(filePath, "creationTime");

            if (validate(urlElements, createdTime.toMillis())) {
                return filePath.toFile();
            }
        }
        return null;
    }

    private String getImageCategoryType(String type) {

        switch (type) {
        case "i":
            return IDP;

        case "a":
            return SP;

        case "u":
            return USER;

        default:
            return DEFAULT;
        }
    }

    private void deleteImageFile(String[] urlElements, String type, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String imageCategoryType = getImageCategoryType(type);
        Path fileStorageLocation = createStorageDirectory(imageCategoryType, tenantId, urlElements[0]);
        if (fileStorageLocation != null) {
            String fileName = urlElements[0];
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            FileTime createdTime = (FileTime) Files.getAttribute(filePath, "creationTime");

            if (validate(urlElements, createdTime.toMillis())) {
                Files.deleteIfExists(filePath);
            }
        }
    }

    private void storeMediaMetadata(Path targetLocation, String fileName, String fileContentType, String fileTag,
                                    FileSecurity fileSecurity, String timestamp) throws IOException {

        Path metadataTargetLocation = targetLocation.resolveSibling(targetLocation.getFileName() + METADATA_FILE_SUFFIX
                + METADATA_FILE_EXTENSION);

        JSONObject metadata = new JSONObject();

        if (StringUtils.isNotBlank(fileName)) {
            metadata.put(METADATA_FILE_NAME, fileName);
        }
        if (StringUtils.isNotBlank(fileContentType)) {
            metadata.put(METADATA_FILE_CONTENT_TYPE, fileContentType);
        }
        if (StringUtils.isNotBlank(fileTag)) {
            metadata.put(METADATA_FILE_TAG, fileTag);
        }
        storeFileSecurityMetadata(fileSecurity, metadata);
        metadata.put(METADATA_FILE_CREATED_TIME, timestamp);
        metadata.put(METADATA_FILE_LAST_ACCESSED_TIME, timestamp);

        try (FileOutputStream fileStream = new FileOutputStream(metadataTargetLocation.toFile());
             Writer writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8)) {
            writer.write(metadata.toJSONString());
        }
    }

    private void storeFileSecurityMetadata(FileSecurity fileSecurity, JSONObject metadata) {

        JSONObject fileSecurityJSON = new JSONObject();

        if (fileSecurity == null) {
            fileSecurityJSON.put(FILE_SECURITY_ALLOWED_ALL, false);
            fileSecurityJSON.put(FILE_SECURITY_ALLOWED_USERS,
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername());
        } else {
            boolean allowedAll = fileSecurity.isAllowedAll();
            fileSecurityJSON.put(FILE_SECURITY_ALLOWED_ALL, allowedAll);

            // Set allowed users and scopes that are required to access protected media.
            if (!allowedAll) {
                List<String> allowedUsers = fileSecurity.getAllowedUsers();
                List<String> allowedScopes = fileSecurity.getAllowedScopes();

 /* Set access level permissions only to the user uploading the media if no allowed users or scopes are
 present in the request. */
                if (CollectionUtils.isEmpty(allowedUsers) && CollectionUtils.isEmpty(allowedScopes)) {
                    fileSecurityJSON.put(FILE_SECURITY_ALLOWED_USERS, PrivilegedCarbonContext
                            .getThreadLocalCarbonContext().getUsername());
                } else {
                    if (CollectionUtils.isNotEmpty(allowedUsers)) {
                        fileSecurityJSON.put(FILE_SECURITY_ALLOWED_USERS, allowedUsers);
                    }
                    if (CollectionUtils.isNotEmpty(allowedScopes)) {
                        fileSecurityJSON.put(FILE_SECURITY_ALLOWED_SCOPES, allowedScopes);
                    }
                }
            }
        }

        metadata.put(METADATA_FILE_SECURITY, fileSecurityJSON);
    }

}

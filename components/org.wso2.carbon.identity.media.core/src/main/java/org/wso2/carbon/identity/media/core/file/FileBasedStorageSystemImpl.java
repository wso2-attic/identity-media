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
package org.wso2.carbon.identity.media.core.file;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.media.core.DataContent;
import org.wso2.carbon.identity.media.core.FileContentImpl;
import org.wso2.carbon.identity.media.core.StorageSystem;
import org.wso2.carbon.identity.media.core.exception.StorageSystemClientException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemServerException;
import org.wso2.carbon.identity.media.core.model.FileSecurity;
import org.wso2.carbon.identity.media.core.model.MediaFileDownloadData;
import org.wso2.carbon.identity.media.core.model.MediaInformation;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.core.util.StorageSystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.CONFIGURABLE_MEDIA_MOUNT_LOCATION;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_CONTENT_TYPE;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_NAME;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_RESOURCE_OWNER_ID;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_SECURITY;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_SECURITY_ALLOWED_ALL;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_SECURITY_ALLOWED_USER_IDS;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.MEDIA_TAG;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.METADATA_FILE_EXTENSION;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.METADATA_FILE_SUFFIX;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.PRE_CREATED_MEDIA_FOLDER;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.PROTECTED_DOWNLOAD_ACCESS;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.PUBLIC_DOWNLOAD_ACCESS;
import static org.wso2.carbon.identity.media.core.util.StorageSystemConstants.SYSTEM_PROPERTY_CARBON_HOME;

/**
 * This is the implementation class to store, retrieve and delete media in the local file system.
 */
public class FileBasedStorageSystemImpl implements StorageSystem {

    private static final Log LOGGER = LogFactory.getLog(FileBasedStorageSystemImpl.class);

    @Override
    public String addMedia(List<InputStream> inputStreams, MediaMetadata mediaMetadata, String uuid,
                           String tenantDomain) throws StorageSystemServerException {

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Uploading media file with unique id: %s and in tenant domain: %s", uuid,
                        tenantDomain));
            }
            return uploadMediaUsingChannels(inputStreams, mediaMetadata, uuid, tenantDomain);
        } catch (IOException e) {
            throw new StorageSystemServerException("Error while uploading media to file system.", e);
        }
    }

    @Override
    public DataContent getFile(String id, String tenantDomain, String type) throws StorageSystemServerException,
            StorageSystemClientException {

        FileContentImpl fileContent = null;
        try {
            MediaFileDownloadData mediaFileDownloadData = getMediaFile(id, tenantDomain, type);
            if (mediaFileDownloadData != null && mediaFileDownloadData.getMediaFile() != null) {
                fileContent = new FileContentImpl(mediaFileDownloadData.getMediaFile(),
                        mediaFileDownloadData.getResponseContentType());
            }
            if (fileContent == null) {
                throw new StorageSystemClientException(String.format("Requested media of type: %s with id: %s in" +
                        " tenant domain: %s is not exiting in the storage system.", type, id, tenantDomain));
            }
            return fileContent;
        } catch (IOException e) {
            throw new StorageSystemServerException("Error while retrieving the stored file", e);
        } catch (ParseException e) {
            String errorMsg = String.format("Unable to parse metadata in JSON format for stored file with id : %s " +
                    "and of type %s in tenant domain: %s", id, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public boolean isDownloadAllowedForPublicMedia(String id, String type, String tenantDomain) throws
            StorageSystemServerException {

        File file;
        try {
            file = getMediaMetadataFile(id, type, tenantDomain);
            if (file != null && file.exists()) {
                return isFileAccessPublic(file);
            }
            return false;
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving metadata for stored file with id: %s and of type " +
                    "%s in tenant domain: %s", id, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        } catch (ParseException e) {
            String errorMsg = String.format("Unable to parse metadata in JSON format for stored file with id : %s " +
                    "and of type %s in tenant domain: %s", id, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public boolean isDownloadAllowedForProtectedMedia(String mediaId, String type, String tenantDomain, String userId)
            throws StorageSystemServerException {

        File file;
        try {
            file = getMediaMetadataFile(mediaId, type, tenantDomain);
            if (file != null && file.exists()) {
                return isFileAccessAllowed(file, userId);
            }
            return false;
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving metadata for stored file with id: %s and of type " +
                    "%s in tenant domain: %s", mediaId, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        } catch (ParseException e) {
            String errorMsg = String.format("Unable to parse metadata in JSON format for stored file with id : %s " +
                    "and of type %s in tenant domain: %s", mediaId, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public boolean isMediaManagementAllowedForEndUser(String mediaId, String type, String tenantDomain, String userId)
            throws StorageSystemServerException {

        File file;
        try {
            file = getMediaMetadataFile(mediaId, type, tenantDomain);
            if (file != null && file.exists()) {
                return isMediaManagementAllowed(file, userId);
            }
            return false;
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving metadata for stored file with id: %s and of type " +
                    "%s in tenant domain: %s", mediaId, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        } catch (ParseException e) {
            String errorMsg = String.format("Unable to parse metadata in JSON format for stored file with id : %s " +
                    "and of type %s in tenant domain: %s", mediaId, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public void deleteMedia(String id, String type, String tenantDomain) throws StorageSystemServerException,
            StorageSystemClientException {

        try {
            deleteFile(id, type, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while deleting the stored file of type %s.", type);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public MediaInformation getMediaInformation(String id, String type, String tenantDomain) throws
            StorageSystemServerException, StorageSystemClientException {

        File file;
        try {
            file = getMediaMetadataFile(id, type, tenantDomain);
            if (file == null || !file.exists()) {
                throw new StorageSystemClientException(String.format("Media information retrieval request can't be" +
                        " performed as media with id: %s of type: %s in tenant domain: %s is not existing.", id, type,
                        tenantDomain));
            }
            return getMediaInformation(file, type, id);
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving metadata for stored file with id: %s and of type " +
                    "%s in tenant domain: %s", id, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        } catch (ParseException e) {
            String errorMsg = String.format("Unable to parse metadata in JSON format for stored file with id : %s " +
                    "and of type %s in tenant domain: %s", id, type, tenantDomain);
            throw new StorageSystemServerException(errorMsg, e);
        }
    }

    @Override
    public InputStream transform(String id, String type, String tenantDomain, InputStream inputStream)
            throws StorageSystemException {
        return inputStream;
    }

    private String uploadMediaUsingChannels(List<InputStream> fileInputStreams, MediaMetadata mediaMetadata,
                                            String uuid, String tenantDomain) throws IOException,
            StorageSystemServerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String fileContentType = mediaMetadata.getFileContentType();
        String fileType = fileContentType.split("/")[0];
        String fileTag = mediaMetadata.getFileTag();
        String fileName = mediaMetadata.getFileName();
        String resourceOwner = mediaMetadata.getResourceOwnerId();
        FileSecurity fileSecurity = mediaMetadata.getFileSecurity();

        Path mediaStoragePath = createStorageDirectory(fileType, tenantId, uuid);

        if (mediaStoragePath != null) {
            Path targetLocation = mediaStoragePath.resolve(uuid);
            File file = targetLocation.toFile();
            // Currently, only single file upload is allowed.
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                 FileChannel fileChannel = fileOutputStream.getChannel();
                 ReadableByteChannel readableByteChannel = Channels.newChannel(fileInputStreams.get(0))) {
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Uploaded media file: %s to directory: %s, for tenant id: %d and type:" +
                                    " %s successfully.", uuid, mediaStoragePath.toString(), tenantId, fileType));
                }
            }

            storeMediaMetadata(targetLocation, fileName, fileContentType, fileTag, resourceOwner, fileSecurity);

            return uuid;
        }
        // TODO: 11/29/19 Add proper warnings for e.g Disk Full /File permission scenarios.

        return "";
    }

    private Path createStorageDirectory(String fileType, int tenantId, String uuid) throws IOException,
            StorageSystemServerException {

        Path fileStorageLocation = getMediaFolderPreCreatedLocation();
        Path mediaPath = null;

        if (fileStorageLocation != null) {
            mediaPath = Files.createDirectories(fileStorageLocation.resolve(Paths.get(fileType,
                    String.valueOf(tenantId))));
        }

        return createUniqueDirectoryStructure(mediaPath, uuid);
    }

    private Path createUniqueDirectoryStructure(Path path, String uuid) throws IOException {

        String[] uuidSplit = uuid.split("-");
        Path uniquePath;
        if (path != null) {
            uniquePath = path;
            for (String pathComponent : uuidSplit) {
                uniquePath = uniquePath.resolve(pathComponent);
            }
            return Files.createDirectories(uniquePath);
        }
        return null;
    }

    private MediaFileDownloadData getMediaFile(String uuid, String tenantDomain, String type) throws
            IOException, ParseException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        MediaFileDownloadData mediaFileDownloadData = new MediaFileDownloadData();

        Path fileStorageLocation = getStorageDirectory(type, tenantId, uuid);
        if (fileStorageLocation != null) {
            Path filePath = fileStorageLocation.resolve(uuid).normalize();
            if (filePath.toFile().exists()) {
                mediaFileDownloadData.setMediaFile(filePath.toFile());
                String metadataFileName = uuid + METADATA_FILE_SUFFIX + METADATA_FILE_EXTENSION;
                Path metadataFilePath = fileStorageLocation.resolve(metadataFileName).normalize();
                if (metadataFilePath.toFile().exists()) {
                    File metadataFile = metadataFilePath.toFile();
                    mediaFileDownloadData.setResponseContentType(getResponseContentTypeFromMetadata(metadataFile));
                }
                return mediaFileDownloadData;
            }
        }
        return null;
    }

    private String getResponseContentTypeFromMetadata(File metadataFile) throws IOException, ParseException {

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(metadataFile),
                StandardCharsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            JSONObject metadata = (JSONObject) parser.parse(reader);
            return (String) metadata.get(MEDIA_CONTENT_TYPE);
        }
    }

    private File getMediaMetadataFile(String uuid, String mediaType, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Path fileStorageLocation = getStorageDirectory(mediaType, tenantId, uuid);
        if (fileStorageLocation != null) {
            String metadataFileName = uuid + METADATA_FILE_SUFFIX + METADATA_FILE_EXTENSION;
            Path filePath = fileStorageLocation.resolve(metadataFileName).normalize();
            return filePath.toFile();
        }
        return null;
    }

    private boolean isFileAccessPublic(File file) throws IOException, ParseException {

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JSONParser jsonParser = new JSONParser();
            Object metadata = jsonParser.parse(reader);
            HashMap fileSecurity = (HashMap) ((JSONObject) metadata).get(MEDIA_SECURITY);
            if (fileSecurity != null) {
                return (Boolean) fileSecurity.get(MEDIA_SECURITY_ALLOWED_ALL);
            }
        }
        return false;
    }

    private boolean isFileAccessAllowed(File file, String userId) throws IOException, ParseException {

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JSONParser jsonParser = new JSONParser();
            Object metadata = jsonParser.parse(reader);
            HashMap fileSecurity = (HashMap) ((JSONObject) metadata).get(MEDIA_SECURITY);
            if (fileSecurity != null) {
                if ((Boolean) fileSecurity.get(MEDIA_SECURITY_ALLOWED_ALL)) {
                    return true;
                }
                return isUserAllowed(fileSecurity, userId);
            }
            return false;
        }
    }

    private boolean isMediaManagementAllowed(File file, String userId) throws IOException, ParseException {

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JSONParser jsonParser = new JSONParser();
            Object metadata = jsonParser.parse(reader);
            String resourceOwner = (String) ((JSONObject) metadata).get(MEDIA_RESOURCE_OWNER_ID);
            return StringUtils.isNotBlank(resourceOwner) && resourceOwner.equals(userId);
        }
    }

    private boolean isUserAllowed(HashMap fileSecurity, String userId) {

        ArrayList allowedUserIds = (ArrayList) fileSecurity.get(MEDIA_SECURITY_ALLOWED_USER_IDS);
        if (allowedUserIds != null) {
            for (Object allowedUserId : allowedUserIds) {
                if (allowedUserId instanceof String) {
                    String user = (String) allowedUserId;
                    if (user.equals(userId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private MediaInformation getMediaInformation(File file, String type, String id) throws IOException, ParseException {

        MediaInformation mediaInformation = new MediaInformation();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JSONParser jsonParser = new JSONParser();
            Object metadata = jsonParser.parse(reader);

            HashMap fileSecurityMap = (HashMap) ((JSONObject) metadata).get(MEDIA_SECURITY);
            boolean allowedAll = (Boolean) fileSecurityMap.get(MEDIA_SECURITY_ALLOWED_ALL);

            ArrayList<String> links = new ArrayList<>();
            String access;
            if (allowedAll) {
                access = PUBLIC_DOWNLOAD_ACCESS;
            } else {
                access = PROTECTED_DOWNLOAD_ACCESS;
            }
            links.add(String.format("/%s/%s/%s", access, type, id));
            mediaInformation.setLinks(links);

            mediaInformation.setMediaMetadata(metadata);
        }
        return mediaInformation;
    }

    private void storeMediaMetadata(Path targetLocation, String fileName, String fileContentType, String fileTag,
                                    String resourceOwner, FileSecurity fileSecurity) throws IOException {

        Path metadataTargetLocation = targetLocation.resolveSibling(targetLocation.getFileName() + METADATA_FILE_SUFFIX
                + METADATA_FILE_EXTENSION);

        JSONObject metadata = new JSONObject();

        if (StringUtils.isNotBlank(fileName)) {
            metadata.put(MEDIA_NAME, fileName);
        }
        if (StringUtils.isNotBlank(fileContentType)) {
            metadata.put(MEDIA_CONTENT_TYPE, fileContentType);
        }
        if (StringUtils.isNotBlank(fileTag)) {
            metadata.put(MEDIA_TAG, fileTag);
        }
        if (StringUtils.isNotBlank(resourceOwner)) {
            metadata.put(MEDIA_RESOURCE_OWNER_ID, resourceOwner);
        }
        storeFileSecurityMetadata(fileSecurity, metadata);

        try (FileOutputStream fileStream = new FileOutputStream(metadataTargetLocation.toFile());
             Writer writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8)) {
            writer.write(metadata.toJSONString());
        }
    }

    private void storeFileSecurityMetadata(FileSecurity fileSecurity, JSONObject metadata) {

        JSONObject fileSecurityJSON = new JSONObject();

        boolean allowedAll = fileSecurity.isAllowedAll();
        fileSecurityJSON.put(MEDIA_SECURITY_ALLOWED_ALL, allowedAll);

        if (!allowedAll) {
            List<String> allowedUserIds = fileSecurity.getAllowedUserIds();
            if (CollectionUtils.isNotEmpty(allowedUserIds)) {
                fileSecurityJSON.put(MEDIA_SECURITY_ALLOWED_USER_IDS, allowedUserIds);
            }
        }
        metadata.put(MEDIA_SECURITY, fileSecurityJSON);
    }

    private void deleteFile(String id, String type, String tenantDomain) throws IOException,
            StorageSystemClientException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        Path fileStorageLocation = getStorageDirectory(type, tenantId, id);
        if (fileStorageLocation == null) {
            throw new StorageSystemClientException(String.format("Delete request cannot be performed as media with " +
                            "id: %s of type: %s in tenant domain: %s not found.", id, type, tenantDomain));
        }

        Path filePath = fileStorageLocation.resolve(id).normalize();
        String metadataFileName = id + METADATA_FILE_SUFFIX + METADATA_FILE_EXTENSION;
        Path metadataFilePath = fileStorageLocation.resolve(metadataFileName).normalize();
        if (Files.notExists(filePath)) {
            throw new StorageSystemClientException(String.format("Delete request cannot be performed as media with " +
                    "id: %s of type: %s in tenant domain: %s not found.", id, type, tenantDomain));
        }

        Files.delete(filePath);
        Files.delete(metadataFilePath);
    }

    private Path getStorageDirectory(String fileType, int tenantId, String id) {

        Path fileStorageLocation = getFileStorageLocation(fileType);
        Path mediaPath = null;
        if (fileStorageLocation != null) {
            if (Files.notExists(fileStorageLocation)) {
                return null;
            }
            mediaPath = fileStorageLocation.resolve(String.valueOf(tenantId));
            if (Files.notExists(mediaPath)) {
                return null;
            }
        }
        return getUniqueDirectoryStructure(mediaPath, id);
    }

    private Path getFileStorageLocation(String fileType) {

        Path fileStorageLocation = null;
        Path configurableRootFolder = getMediaMountBaseDirectory();

        if (configurableRootFolder != null) {
            fileStorageLocation = configurableRootFolder.resolve(Paths.get(PRE_CREATED_MEDIA_FOLDER, fileType));
        }
        return fileStorageLocation;
    }

    private Path getMediaFolderPreCreatedLocation() throws StorageSystemServerException {

        Path fileStorageLocation = null;
        Path configurableRootFolder = getMediaMountBaseDirectory();

        if (configurableRootFolder != null) {
            fileStorageLocation = configurableRootFolder.resolve(Paths.get(PRE_CREATED_MEDIA_FOLDER));

            // The system expects a pre-created folder called 'media' to exist in the configured media mount base
            // directory.
            if (Files.notExists(fileStorageLocation)) {
                throw new StorageSystemServerException(String.format("A pre-created \"media\" folder within the " +
                        "configured media mount base directory %s is expected.", configurableRootFolder));
            }
        }
        return fileStorageLocation;
    }

    private Path getMediaMountBaseDirectory() {

        Path configurableRootFolder = null;
        String environmentVariableForRootFolder = System.getenv(CONFIGURABLE_MEDIA_MOUNT_LOCATION);
        if (StringUtils.isNotBlank(environmentVariableForRootFolder)) {
            configurableRootFolder = Paths.get(environmentVariableForRootFolder);
        }

        if (configurableRootFolder == null) {
            configurableRootFolder = Paths.get(System.getProperty(SYSTEM_PROPERTY_CARBON_HOME),
                    StorageSystemUtil.getMediaMountLocation());
        }
        return configurableRootFolder;
    }

    private Path getUniqueDirectoryStructure(Path path, String uuid) {

        String[] uuidSplit = uuid.split("-");
        Path uniquePath;
        if (path != null) {
            uniquePath = path;
            for (String pathComponent : uuidSplit) {
                uniquePath = uniquePath.resolve(pathComponent);
            }
            if (Files.exists(uniquePath)) {
                return uniquePath;
            }
        }
        return null;
    }

}

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
package org.wso2.carbon.identity.image.file;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.image.StorageSystem;
import org.wso2.carbon.identity.image.exception.StorageSystemException;
import org.wso2.carbon.identity.image.util.StorageSystemUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import static org.wso2.carbon.identity.image.util.StorageSystemConstants.CONFIGURABLE_UPLOAD_LOCATION;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.DEFAULT;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.FILE_CREATION_TIME_ATTRIBUTE;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.IDP;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.ID_SEPERATOR;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.IMAGE_STORE;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.SP;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.SYSTEM_PROPERTY_CARBON_HOME;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.USER;

/**
 * This is the implementation class to store images in to local file system.
 */
public class FileBasedStorageSystemImpl implements StorageSystem {

    private static final Log LOGGER = LogFactory.getLog(FileBasedStorageSystemFactory.class);

    @Override
    public String addFile(InputStream inputStream, String type, String uuid, String tenantDomain)
            throws StorageSystemException {

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        String.format("Uploading image file of type %s, unique id of %s and in tenant domain %s", type,
                                uuid, tenantDomain));
            }
            return uploadImageUsingChannels(inputStream, type, uuid, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while uploading image to file system for %s type.", type);
            throw new StorageSystemException(errorMsg, e);
        }

    }

    @Override
    public byte[] getFile(String id, String type, String tenantDomain) throws StorageSystemException {

        String[] imageUniqueIdElements = retrieveImageUniqueIdElements(id);
        InputStream inputStream;
        try {
            inputStream = getImageFile(imageUniqueIdElements, type, tenantDomain);
            if (inputStream != null) {
                return IOUtils.toByteArray(inputStream);
            }
            return new byte[0];
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

    private String uploadImageUsingChannels(InputStream fileInputStream, String type, String uuid, String tenantDomain)
            throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Path imagesPath = createStorageDirectory(type, tenantId, uuid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Uploading image file to directory %s, for tenant id %d", imagesPath.toString(),
                    tenantId));
        }
        if (imagesPath != null) {
            Path targetLocation = imagesPath.resolve(uuid);
            try (FileOutputStream fileOutputStream = new FileOutputStream(targetLocation.toFile());
                    FileChannel fileChannel = fileOutputStream.getChannel();
                    ReadableByteChannel readableByteChannel = Channels.newChannel(fileInputStream)) {
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                if (LOGGER.isDebugEnabled()) {
                    byte[] imageByteCount = IOUtils.toByteArray(fileInputStream);
                    LOGGER.debug(String.format("Writing image data of size %d to a file named %s at location %s.",
                            imageByteCount.length, uuid, targetLocation.toString()));
                }
            }

            FileTime createdTime = (FileTime) Files.getAttribute(targetLocation, FILE_CREATION_TIME_ATTRIBUTE);
            String timeStampAsString = Long.toString(createdTime.toMillis());
            String uuidHash = StorageSystemUtil.calculateUUIDHash(uuid, timeStampAsString);

            return uuid + ID_SEPERATOR + uuidHash + ID_SEPERATOR + timeStampAsString;
        }
        // TODO: 11/29/19 Add proper warnings for e.g Disk Full /File permission scenarios.

        return "";
    }

    private Path createStorageDirectory(String type, int tenantId, String uuid) throws IOException {

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
            fileStorageLocation = configurableRootFolder.resolve(Paths.get(IMAGE_STORE));
        }

        Path imagesPath = null;

        if (fileStorageLocation != null) {
            switch (type) {
            case IDP:
                imagesPath = Files
                        .createDirectories(fileStorageLocation.resolve(IDP).resolve(String.valueOf(tenantId)));
                break;

            case SP:
                imagesPath = Files.createDirectories(fileStorageLocation.resolve(SP).resolve(String.valueOf(tenantId)));
                break;

            case USER:
                imagesPath = Files
                        .createDirectories(fileStorageLocation.resolve(USER).resolve(String.valueOf(tenantId)));
                break;

            default:
                imagesPath = Files
                        .createDirectories(fileStorageLocation.resolve(DEFAULT).resolve(String.valueOf(tenantId)));
            }
        }

        return createUniqueDirectoryStructure(imagesPath, uuid);

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

    private InputStream getImageFile(String[] urlElements, String type, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String imageCategoryType = getImageCategoryType(type);
        Path fileStorageLocation = createStorageDirectory(imageCategoryType, tenantId, urlElements[0]);
        if (fileStorageLocation != null) {
            String fileName = urlElements[0];
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            FileTime createdTime = (FileTime) Files.getAttribute(filePath, "creationTime");

            if (validate(urlElements, createdTime.toMillis())) {
                return Files.newInputStream(filePath);
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

}

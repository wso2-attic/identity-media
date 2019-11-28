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

import static org.wso2.carbon.identity.image.util.StorageSystemConstants.ID_SEPERATOR;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.IMAGE_STORE;
import static org.wso2.carbon.identity.image.util.StorageSystemConstants.SYSTEM_PROPERTY_CARBON_HOME;

/**
 * This is the implementation class to store images in to local file system.
 */
public class FileBasedStorageSystemImpl implements StorageSystem {

    private static final Log LOGGER = LogFactory.getLog(FileBasedStorageSystemFactory.class);

    @Override
    public String addFile(InputStream inputStream, String type, String uuid, String tenantDomain)
            throws StorageSystemException {

        try {
            return uploadImageUsingChannels(inputStream, type, uuid, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while uploading image to file system for %s type.", type);
            throw new StorageSystemException(errorMsg, e);
        }

    }

    @Override
    public InputStream getFile(String id, String type, String tenantDomain) throws StorageSystemException {

        String[] urlElements = retrieveUrlElements(id);
        InputStream inputStream;
        try {
            inputStream = getImageFile(urlElements, type, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while retrieving the stored file of type %s.", type);
            throw new StorageSystemException(errorMsg, e);
        }
        return inputStream;
    }

    @Override
    public void deleteFile(String id, String type, String tenantDomain) throws StorageSystemException {

        String[] urlElements = retrieveUrlElements(id);
        try {
            deleteImageFile(urlElements, type, tenantDomain);
        } catch (IOException e) {
            String errorMsg = String.format("Error while deleting the stored file of type %s.", type);
            throw new StorageSystemException(errorMsg, e);
        }

    }

    private String uploadImageUsingChannels(InputStream fileInputStream, String type, String uuid, String tenantDomain)
            throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        Path imagesPath = createSpecificDirectory(type, tenantId);
        if (imagesPath != null) {
            Path targetLocation = imagesPath.resolve(uuid);
            if (targetLocation != null) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(targetLocation.toFile());
                        FileChannel fileChannel = fileOutputStream.getChannel();
                        ReadableByteChannel readableByteChannel = Channels.newChannel(fileInputStream)) {
                    fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }

                FileTime createdTime = (FileTime) Files.getAttribute(targetLocation, "creationTime");
                String timeStampAsString = Long.toString(createdTime.toMillis());
                String uuidHash = new StorageSystemUtil().calculateUUIDHash(uuid, timeStampAsString);

                return uuid + ID_SEPERATOR + uuidHash + ID_SEPERATOR + timeStampAsString;
            }
        }

        return null;
    }

    private Path createSpecificDirectory(String type, int tenantId) throws IOException {

        Path carbonHomeLocation = Paths.get(System.getProperty(SYSTEM_PROPERTY_CARBON_HOME));
        Path fileStorageLocation = null;
        if (carbonHomeLocation != null) {
            fileStorageLocation = carbonHomeLocation.resolve(Paths.get(IMAGE_STORE));
        }

        if (fileStorageLocation != null) {
            switch (type) {
            case "idp":
                return Files.createDirectories(fileStorageLocation.resolve("idp").resolve(String.valueOf(tenantId)));

            case "app":
                return Files.createDirectories(fileStorageLocation.resolve("app").resolve(String.valueOf(tenantId)));

            case "user":
                return Files.createDirectories(fileStorageLocation.resolve("user").resolve(String.valueOf(tenantId)));

            default:
                return Files
                        .createDirectories(fileStorageLocation.resolve("default").resolve(String.valueOf(tenantId)));
            }
        }

        return null;

    }

    private Path generateImageCategoryPath(String type) {

        Path fileStorageLocation = Paths.get(System.getProperty(SYSTEM_PROPERTY_CARBON_HOME))
                .resolve(Paths.get(IMAGE_STORE));

        switch (type) {
        case "idp":
            return fileStorageLocation.resolve("idp");

        case "app":
            return fileStorageLocation.resolve("app");

        case "user":
            return fileStorageLocation.resolve("user");

        default:
            return fileStorageLocation.resolve("default");
        }
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
    private String[] retrieveUrlElements(String id) {

        if (id == null) {
            return new String[0];
        }
        return id.split(ID_SEPERATOR);

    }

    private boolean validate(String[] urlElements, long createdTime) {

        if (urlElements[2].equals(Long.toString(createdTime)) && urlElements[1]
                .equals(new StorageSystemUtil().calculateUUIDHash(urlElements[0], urlElements[2]))) {
            return true;
        }
        return false;

    }

    private InputStream getImageFile(String[] urlElements, String type, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String imageCategoryType = getImageCategoryType(type);
        Path fileStorageLocation = createSpecificDirectory(imageCategoryType, tenantId);
        String fileName = urlElements[0];
        Path filePath = fileStorageLocation.resolve(fileName).normalize();
        FileTime createdTime = (FileTime) Files.getAttribute(filePath, "creationTime");

        if (validate(urlElements, createdTime.toMillis())) {
            InputStream inputStream = Files.newInputStream(filePath);
            return inputStream;
        }
        return null;
    }

    private String getImageCategoryType(String type) {

        switch (type) {
        case "i":
            return "idp";

        case "a":
            return "app";

        case "u":
            return "user";

        default:
            return "default";
        }
    }

    private void deleteImageFile(String[] urlElements, String type, String tenantDomain) throws IOException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        String imageCategoryType = getImageCategoryType(type);
        Path fileStorageLocation = generateImageCategoryPath(imageCategoryType);
        String fileName = urlElements[0];
        Path filePath = fileStorageLocation.resolve(String.valueOf(tenantId)).resolve(fileName).normalize();
        FileTime createdTime = (FileTime) Files.getAttribute(filePath, "creationTime");

        if (validate(urlElements, createdTime.toMillis())) {
            Files.deleteIfExists(filePath);
        }

    }

}

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

import org.apache.log4j.Logger;
import org.wso2.carbon.identity.image.StorageSystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * This is the implementation class to store images in to local file system.
 */
public class FileBasedStorageSystemImpl implements StorageSystem {

    private static final Logger LOGGER = Logger.getLogger(FileBasedStorageSystemFactory.class);
    private static final String SYSTEM_PROPERTY_CARBON_HOME = "carbon.home";
    private static final String IMAGE_STORE = "repository/images";

    @Override
    public void addFile(InputStream inputStream, String type) {
        try {
            uploadImageUsingChannels(inputStream, type);
        } catch (IOException e) {
            LOGGER.error("Error while uploading file. ", e);
            // TODO: 11/22/19 throw custom excpetion related to this component. 
        }
    }

    @Override
    public void getFIle() {

    }

    @Override
    public void deleteFile() {

    }

    private void uploadImageUsingChannels(InputStream fileInputStream, String type) throws IOException {

        String fileName = UUID.randomUUID().toString();
        Path imagesPath;
        Path targetLocation;
        imagesPath = createSpecificDirectory(type);

        targetLocation = imagesPath.resolve(fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(targetLocation.toFile());
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileChannel.transferFrom(Channels.newChannel(fileInputStream), 0, Long.MAX_VALUE);

    }

    private Path createSpecificDirectory(String type) throws IOException {

        Path fileStorageLocation = Paths.get(System.getProperty(SYSTEM_PROPERTY_CARBON_HOME))
                .resolve(Paths.get(IMAGE_STORE));
        switch (type) {
        case "idp":
            return Files.createDirectories(fileStorageLocation.resolve("idp"));

        case "sp":
            return Files.createDirectories(fileStorageLocation.resolve("sp"));

        case "user":
            return Files.createDirectories(fileStorageLocation.resolve("user"));

        default:
            return Files.createDirectories(fileStorageLocation.resolve("default"));
        }

    }

}

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
package org.wso2.carbon.identity.media;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.media.exception.StorageSystemException;
import org.wso2.carbon.identity.media.file.FileBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl;
import org.wso2.carbon.identity.media.internal.MediaServiceDataHolder;
import org.wso2.carbon.identity.media.jdbc.DatabaseBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.model.FileSecurity;
import org.wso2.carbon.identity.media.model.MediaMetadata;
import org.wso2.carbon.identity.media.util.StorageSystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
                        IdentityUtil.class, MediaServiceDataHolder.class, FileBasedStorageSystemImpl.class,
                        StorageSystemUtil.class, IdentityTenantUtil.class
                })
public class StorageSystemManagerTest extends PowerMockTestCase {

    private StorageSystemManager storageSystemManager;
    private MediaServiceDataHolder mediaServiceDataHolder;
    private StorageSystemFactory fileBasedStorageSystemFactory;
    private StorageSystemFactory databaseBasedStorageSystemFactory;

    @BeforeTest
    public void setUp() {

        storageSystemManager = new StorageSystemManager();
        fileBasedStorageSystemFactory = spy(new FileBasedStorageSystemFactory());
        databaseBasedStorageSystemFactory = spy(new DatabaseBasedStorageSystemFactory());
        mediaServiceDataHolder = MediaServiceDataHolder.getInstance();

        mediaServiceDataHolder.getStorageSystemFactories()
                .put("org.wso2.carbon.identity.media.file" + ".FileBasedStorageSystemImpl",
                        fileBasedStorageSystemFactory);
        mediaServiceDataHolder.getStorageSystemFactories()
                .put("org.wso2.carbon.identity.media.jdbc" + ".DatabaseBasedStorageSystemImpl",
                        databaseBasedStorageSystemFactory);
    }

    @Test
    public void testReadFileBasedStorageTypeFromConfig() throws Exception {

        final String storeType = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), storeType);

    }

    @Test
    public void testReadDatabaseBasedStorageTypeFromConfig() throws Exception {

        final String storeType = "org.wso2.carbon.identity.media.jdbc.DatabaseBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), storeType);

    }

    @Test
    public void testReadCustomStorageTypeFromConfig() throws Exception {

        final String storeType = "org.wso2.carbon.identity.image.custom.CustomStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), storeType);

    }

    @Test
    public void testReadEmptyStorageTypeFromConfig() throws Exception {

        final String storeType = "";
        final String defaultStoreType = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), defaultStoreType);

    }

    @Test
    public void testReadNullStorageTypeFromConfig() throws Exception {

        final String storeType = null;
        final String defaultStoreType = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), defaultStoreType);

    }

    @Test
    public void testGetFileBasedStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.media.file.FileBasedStorageSystemImpl";
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType),
                fileBasedStorageSystemFactory);

    }

    @Test
    public void testGetDataBaseBasedStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.media.jdbc.DatabaseBasedStorageSystemImpl";
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType),
                databaseBasedStorageSystemFactory);

    }

    @Test
    public void testGetIncorrectStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.media.incorrect.IncorrectStorageSystemImpl";
        Assert.assertNull(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType));

    }

    @Test
    public void testAddImageUsingFileBasedStorage() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);

        FileBasedStorageSystemImpl fileBasedStorageSystem = spy(new FileBasedStorageSystemImpl());
        when(fileBasedStorageSystemFactory.getInstance()).thenReturn(fileBasedStorageSystem);

        mockStatic(StorageSystemUtil.class);
        String mockUUID = "30d0325e-40bc-45f3-845e-f13dd130e963";
        when(StorageSystemUtil.calculateUUID()).thenReturn(mockUUID);

        ClassLoader classLoader = getClass().getClassLoader();
        File mediaFile = new File(classLoader.getResource("profilepic.png").getFile());
        InputStream fileInputStream = new FileInputStream(mediaFile);
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(fileInputStream);

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.setFileContentType("image/png");
        mediaMetadata.setFileName("profilepic.png");
        mediaMetadata.setFileTag("user");
        boolean allowedAll = false;
        ArrayList<String> allowedUsers = new ArrayList<>();
        allowedUsers.add("user1");
        FileSecurity fileSecurity = new FileSecurity(allowedAll, allowedUsers);
        mediaMetadata.setFileSecurity(fileSecurity);

        mockStatic(IdentityTenantUtil.class);
        String tenantDomain = "carbon.super";
        when(IdentityTenantUtil.getTenantId(tenantDomain)).thenReturn(-1234);
        URL tevaUrl = getClass().getClassLoader().getResource("sampleimage");
        String tevaTestFolder = new File(tevaUrl.toURI()).getAbsolutePath();
        System.setProperty("upload.location", tevaTestFolder);

        String url = storageSystemManager.addFile(inputStreams, mediaMetadata, tenantDomain);
        Assert.assertEquals(url, mockUUID);

    }

    @Test
    public void testAddImageWithIncorrectStorageSystemFactory() throws Exception {

        final String storeType = "org.wso2.carbon.identity.image.custom.CustomStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        InputStream inputStream = mock(InputStream.class);
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(inputStream);
        MediaMetadata mediaMetadata = mock(MediaMetadata.class);
        String tenantDomain = "carbon.super";
        Assert.assertEquals(storageSystemManager.addFile(inputStreams, mediaMetadata, tenantDomain), "");

    }

    @Test
    public void testGetImageUsingFileBasedStorage() throws StorageSystemException {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getInstance()).thenReturn(fileBasedStorageSystem);

        File file = new File("Dummy path");
        FileContentImpl fileContent = new FileContentImpl(file);

        when(fileBasedStorageSystem.getFile(anyString(), anyString(), anyString())).thenReturn(fileContent);
        String id = "imageuuid";
        String type = "idp";
        String tenantDomain = "carbon.super";
        Assert.assertEquals(storageSystemManager.readContent(id, type, tenantDomain), fileContent);
    }

}

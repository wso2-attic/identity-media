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
package org.wso2.carbon.identity.image;

import org.apache.commons.codec.digest.DigestUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.image.exception.StorageSystemException;
import org.wso2.carbon.identity.image.file.FileBasedStorageSystemFactory;
import org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl;
import org.wso2.carbon.identity.image.internal.ImageServiceDataHolder;
import org.wso2.carbon.identity.image.jdbc.DatabaseBasedStorageSystemFactory;
import org.wso2.carbon.identity.image.util.StorageSystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
                        IdentityUtil.class, ImageServiceDataHolder.class, FileBasedStorageSystemImpl.class,
                        StorageSystemUtil.class, IdentityTenantUtil.class
                })
public class StorageSystemManagerTest extends PowerMockTestCase {

    private StorageSystemManager storageSystemManager;
    private ImageServiceDataHolder imageServiceDataHolder;
    private StorageSystemFactory fileBasedStorageSystemFactory;
    private StorageSystemFactory databaseBasedStorageSystemFactory;

    @BeforeTest
    public void setUp() {

        storageSystemManager = new StorageSystemManager();
        fileBasedStorageSystemFactory = spy(new FileBasedStorageSystemFactory());
        databaseBasedStorageSystemFactory = spy(new DatabaseBasedStorageSystemFactory());
        imageServiceDataHolder = ImageServiceDataHolder.getInstance();

        imageServiceDataHolder.getStorageSystemFactories()
                .put("org.wso2.carbon.identity.image.file" + ".FileBasedStorageSystemImpl",
                        fileBasedStorageSystemFactory);
        imageServiceDataHolder.getStorageSystemFactories()
                .put("org.wso2.carbon.identity.image.jdbc" + ".DatabaseBasedStorageSystemImpl",
                        databaseBasedStorageSystemFactory);
    }

    @Test
    public void testReadFileBasedStorageTypeFromConfig() throws Exception {

        final String storeType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), storeType);

    }

    @Test
    public void testReadDatabaseBasedStorageTypeFromConfig() throws Exception {

        final String storeType = "org.wso2.carbon.identity.image.jdbc.DatabaseBasedStorageSystemImpl";
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
        final String defaultStoreType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), defaultStoreType);

    }

    @Test
    public void testReadNullStorageTypeFromConfig() throws Exception {

        final String storeType = null;
        final String defaultStoreType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "readStorageTypeFromConfig"), defaultStoreType);

    }

    @Test
    public void testGetFileBasedStorageSystemFactory() throws Exception {

        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl";
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType),
                fileBasedStorageSystemFactory);

    }

    @Test
    public void testGetDataBaseBasedStorageSystemFactory() throws Exception {

        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.image.jdbc.DatabaseBasedStorageSystemImpl";
        Assert.assertEquals(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType),
                databaseBasedStorageSystemFactory);

    }

    @Test
    public void testGetIncorrectStorageSystemFactory() throws Exception {

        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
        final String storeType = "org.wso2.carbon.identity.image.incorrect.IncorrectStorageSystemImpl";
        Assert.assertNull(Whitebox.invokeMethod(storageSystemManager, "getStorageSystemFactory", storeType));

    }

    @Test
    public void testAddImageUsingFileBasedStorage() throws Exception {
        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
        FileBasedStorageSystemImpl fileBasedStorageSystem = spy(new FileBasedStorageSystemImpl());
        when(fileBasedStorageSystemFactory.getInstance()).thenReturn(fileBasedStorageSystem);
        mockStatic(StorageSystemUtil.class);
        String mockUUID = "30d0325e-40bc-45f3-845e-f13dd130e963";
        when(StorageSystemUtil.calculateUUID()).thenReturn(mockUUID);
        ClassLoader classLoader = getClass().getClassLoader();
        File imageFile = new File(classLoader.getResource("profilepic.png").getFile());
        InputStream fileInputStream = new FileInputStream(imageFile);
        mockStatic(IdentityTenantUtil.class);
        String type = "idp";
        String tenantDomain = "carbon.super";
        when(IdentityTenantUtil.getTenantId(tenantDomain)).thenReturn(-1234);
        URL tevaUrl = getClass().getClassLoader().getResource("sampleimage");
        String tevaTestFolder = new File(tevaUrl.toURI()).getAbsolutePath();
        System.setProperty("upload.location", tevaTestFolder);
        mockStatic(Long.class);
        String timeStampAsString = "1575350130000";
        when(Long.toString(anyLong())).thenReturn(timeStampAsString);
        String uuidHash = DigestUtils.sha256Hex(mockUUID + timeStampAsString);
        when(StorageSystemUtil.calculateUUIDHash(mockUUID, timeStampAsString)).thenReturn(uuidHash);

        String url = storageSystemManager.addFile(fileInputStream, type, tenantDomain);
        Assert.assertEquals(url, mockUUID + "_" + uuidHash + "_" + timeStampAsString);

    }

    @Test
    public void testAddImageWithIncorrectStorageSystemFactory() throws Exception {

        final String storeType = "org.wso2.carbon.identity.image.custom.CustomStorageSystemImpl";
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty("ContentStore.Type")).thenReturn(storeType);
        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
        InputStream inputStream = mock(InputStream.class);
        String type = "idp";
        String tenantDomain = "carbon.super";
        Assert.assertEquals(storageSystemManager.addFile(inputStream, type, tenantDomain), "");

    }

    @Test
    public void testGetImageUsingFileBasedStorage() throws StorageSystemException {

        mockStatic(ImageServiceDataHolder.class);
        when(ImageServiceDataHolder.getInstance()).thenReturn(imageServiceDataHolder);
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

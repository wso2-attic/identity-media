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
package org.wso2.carbon.identity.media.core;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.media.core.exception.StorageSystemClientException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.exception.StorageSystemServerException;
import org.wso2.carbon.identity.media.core.file.FileBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.core.file.FileBasedStorageSystemImpl;
import org.wso2.carbon.identity.media.core.internal.MediaServiceDataHolder;
import org.wso2.carbon.identity.media.core.jdbc.DatabaseBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.core.model.FileSecurity;
import org.wso2.carbon.identity.media.core.model.MediaInformation;
import org.wso2.carbon.identity.media.core.model.MediaMetadata;
import org.wso2.carbon.identity.media.core.util.StorageSystemUtil;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
                        IdentityUtil.class, MediaServiceDataHolder.class, FileBasedStorageSystemImpl.class,
                        StorageSystemUtil.class, IdentityTenantUtil.class, PrivilegedCarbonContext.class
                })
public class StorageSystemManagerTest extends PowerMockTestCase {

    private StorageSystemManager storageSystemManager;
    private MediaServiceDataHolder mediaServiceDataHolder;
    private StorageSystemFactory fileBasedStorageSystemFactory;
    private StorageSystemFactory databaseBasedStorageSystemFactory;

    @BeforeTest
    public void setUp() {

        mockRealmService();

        storageSystemManager = new StorageSystemManager();
        fileBasedStorageSystemFactory = spy(new FileBasedStorageSystemFactory());
        databaseBasedStorageSystemFactory = spy(new DatabaseBasedStorageSystemFactory());
        mediaServiceDataHolder = MediaServiceDataHolder.getInstance();

        mediaServiceDataHolder.getStorageSystemFactories().put(TestConstants.FILE_BASED_MEDIA_STORE,
                        fileBasedStorageSystemFactory);
        mediaServiceDataHolder.getStorageSystemFactories().put(TestConstants.DATABASE_BASED_MEDIA_STORE,
                        databaseBasedStorageSystemFactory);
    }

    @Test
    public void testGetFileBasedStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        Assert.assertNotNull(Whitebox.invokeMethod(storageSystemManager, "getStorageSystem",
                TestConstants.FILE_BASED_MEDIA_STORE));
    }

    @Test
    public void testGetDataBaseBasedStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        Assert.assertNotNull(Whitebox.invokeMethod(storageSystemManager, "getStorageSystem",
                TestConstants.DATABASE_BASED_MEDIA_STORE));
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testGetIncorrectStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        Whitebox.invokeMethod(storageSystemManager, "getStorageSystem", TestConstants.INCORRECT_MEDIA_STORE);
    }

    @Test
    public void testAddMediaUsingFileBasedStorage() throws Exception {

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.calculateUUID()).thenReturn(TestConstants.MEDIA_UUID);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        ClassLoader classLoader = getClass().getClassLoader();
        File mediaFile = new File(classLoader.getResource(TestConstants.FILE_NAME).getFile());
        InputStream fileInputStream = new FileInputStream(mediaFile);
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(fileInputStream);

        MediaMetadata mediaMetadata = new MediaMetadata();
        mediaMetadata.setFileContentType(TestConstants.FILE_CONTENT_TYPE);
        mediaMetadata.setFileName(TestConstants.FILE_NAME);
        mediaMetadata.setFileTag("user");
        boolean allowedAll = false;
        ArrayList<String> allowedUserIds = new ArrayList<>();
        allowedUserIds.add(TestConstants.USER_ID);
        FileSecurity fileSecurity = new FileSecurity(allowedAll, allowedUserIds);
        mediaMetadata.setFileSecurity(fileSecurity);

        when(fileBasedStorageSystem.addMedia(anyList(), any(MediaMetadata.class), anyString(), anyString()))
                .thenReturn(TestConstants.MEDIA_UUID);
        String uuid = storageSystemManager.addFile(inputStreams, mediaMetadata, TestConstants.TENANT_DOMAIN);
        Assert.assertEquals(uuid, TestConstants.MEDIA_UUID);
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testAddMediaWithNoStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        InputStream inputStream = mock(InputStream.class);
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(inputStream);
        MediaMetadata mediaMetadata = mock(MediaMetadata.class);
        storageSystemManager.addFile(inputStreams, mediaMetadata, TestConstants.TENANT_DOMAIN);
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testAddMediaWithInvalidStorageSystemFactory() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        InputStream inputStream = mock(InputStream.class);
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(inputStream);
        MediaMetadata mediaMetadata = mock(MediaMetadata.class);
        storageSystemManager.addFile(inputStreams, mediaMetadata, TestConstants.TENANT_DOMAIN);
    }

    @Test
    public void testGetMediaUsingFileBasedStorage() throws StorageSystemException {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);
        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        File file = new File("Dummy path");
        FileContentImpl fileContent = new FileContentImpl(file);

        when(fileBasedStorageSystem.getFile(anyString(), anyString(), anyString())).thenReturn(fileContent);

        Assert.assertEquals(storageSystemManager.readContent(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN), fileContent);
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testGetMediaWithIncorrectStorageSystemFactory() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);

        storageSystemManager.readContent(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN);
    }

    @Test
    public void testRetrieveMediaInformationUsingFileBasedStorage() throws Exception {

        mockStatic(MediaServiceDataHolder.class);
        when(MediaServiceDataHolder.getInstance()).thenReturn(mediaServiceDataHolder);

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        MediaInformation mediaInformation = new MediaInformation();

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);
        when(fileBasedStorageSystem.getMediaInformation(anyString(), anyString(), anyString()))
                .thenReturn(mediaInformation);

        Assert.assertEquals(storageSystemManager.retrieveMediaInformation(TestConstants.MEDIA_UUID,
                TestConstants.MEDIA_TYPE, TestConstants.TENANT_DOMAIN), mediaInformation);
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testRetrieveMediaInformationWithIncorrectStorageSystemFactory() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        storageSystemManager.retrieveMediaInformation(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN);
    }

    @Test
    public void testIsDownloadAllowedForPublicMediaUsingFileBasedStorage() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);
        when(fileBasedStorageSystem.isDownloadAllowedForPublicMedia(anyString(), anyString(), anyString()))
                .thenReturn(true);

        Assert.assertTrue(storageSystemManager.isDownloadAllowedForPublicMedia(TestConstants.MEDIA_UUID,
                TestConstants.MEDIA_TYPE, TestConstants.TENANT_DOMAIN));
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testIsDownloadAllowedForPublicMediaWithIncorrectStorageSystemFactory() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        storageSystemManager.isDownloadAllowedForPublicMedia(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN);
    }

    @Test
    public void testIsDownloadAllowedForProtectedMediaUsingFileBasedStorage() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);
        when(fileBasedStorageSystem.isDownloadAllowedForProtectedMedia(anyString(), anyString(), anyString(),
                anyString())).thenReturn(true);

        try {
            startTenantFlow();
            Assert.assertTrue(storageSystemManager.isDownloadAllowedForProtectedMedia(TestConstants.MEDIA_UUID,
                    TestConstants.MEDIA_TYPE, TestConstants.TENANT_DOMAIN, TestConstants.USER_ID));
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testIsDownloadAllowedForProtectedMediaWithIncorrectStorageSystemFactory() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        Assert.assertFalse(storageSystemManager.isDownloadAllowedForProtectedMedia(TestConstants.MEDIA_UUID,
                TestConstants.MEDIA_TYPE, TestConstants.TENANT_DOMAIN, TestConstants.USER_ID));
    }

    @Test
    public void testIsMediaManagementAllowedForEndUserUsingFileBasedStorage() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);
        when(fileBasedStorageSystem.isMediaManagementAllowedForEndUser(anyString(), anyString(), anyString(),
                anyString())).thenReturn(true);

        try {
            startTenantFlow();
            Assert.assertTrue(storageSystemManager.isMediaManagementAllowedForEndUser(TestConstants.MEDIA_UUID,
                    TestConstants.MEDIA_TYPE, TestConstants.TENANT_DOMAIN, TestConstants.USER_ID));
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testIsMediaManagementAllowedForEndUserWithIncorrectStorageSystemFactory() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        storageSystemManager.isMediaManagementAllowedForEndUser(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN, TestConstants.USER_ID);
    }

    @Test
    public void testValidateMediaTypePathParam() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        contentTypes.put(TestConstants.MEDIA_TYPE, null);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateMediaTypePathParam(TestConstants.MEDIA_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateMediaTypePathParamWhenContentTypesConfiguredIsEmpty() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateMediaTypePathParam(TestConstants.MEDIA_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateMediaTypePathParamNotAvailableInConfiguredContentTypes() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        contentTypes.put(TestConstants.MEDIA_TYPE, null);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateMediaTypePathParam(TestConstants.INVALID_MEDIA_TYPE_PATH_PARAM);
    }

    @Test
    public void testValidateFileUploadMediaTypes() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        List<String> contentSubTypes = new ArrayList<>();
        contentSubTypes.add(TestConstants.CONTENT_SUB_TYPE);
        contentTypes.put(TestConstants.MEDIA_TYPE, contentSubTypes);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateFileUploadMediaTypes(TestConstants.MEDIA_TYPE, TestConstants.CONTENT_SUB_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateFileUploadMediaTypesForUnallowedContentType() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        List<String> contentSubTypes = new ArrayList<>();
        contentSubTypes.add(TestConstants.CONTENT_SUB_TYPE);
        contentTypes.put(TestConstants.MEDIA_TYPE, contentSubTypes);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateFileUploadMediaTypes(TestConstants.INVALID_MEDIA_TYPE,
                TestConstants.CONTENT_SUB_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateFileUploadMediaTypesForNoConfiguredContentTypes() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getContentTypes()).thenReturn(null);

        storageSystemManager.validateFileUploadMediaTypes(TestConstants.MEDIA_TYPE, TestConstants.CONTENT_SUB_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateFileUploadMediaTypesForUnallowedContentSubType() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        List<String> contentSubTypes = new ArrayList<>();
        contentSubTypes.add(TestConstants.CONTENT_SUB_TYPE);
        contentTypes.put(TestConstants.MEDIA_TYPE, contentSubTypes);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateFileUploadMediaTypes(TestConstants.MEDIA_TYPE,
                TestConstants.INVALID_CONTENT_SUB_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateFileUploadMediaTypesForNoConfiguredContentSubTypes() throws Exception {

        mockStatic(StorageSystemUtil.class);
        HashMap<String, List<String>> contentTypes = new HashMap<>();
        contentTypes.put(TestConstants.MEDIA_TYPE, null);
        when(StorageSystemUtil.getContentTypes()).thenReturn(contentTypes);

        storageSystemManager.validateFileUploadMediaTypes(TestConstants.MEDIA_TYPE, TestConstants.CONTENT_SUB_TYPE);
    }

    @Test(expectedExceptions = StorageSystemClientException.class)
    public void testValidateInvalidMediaSize() throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();
        File mediaFile = new File(classLoader.getResource(TestConstants.FILE_NAME).getFile());
        InputStream fileInputStream = new FileInputStream(mediaFile);
        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaMaximumSize()).thenReturn(100);

        storageSystemManager.validateMediaSize(fileInputStream);
    }

    @Test
    public void testIsMediaDeletedUsingFileBasedStorage() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.FILE_BASED_MEDIA_STORE);

        FileBasedStorageSystemImpl fileBasedStorageSystem = mock(FileBasedStorageSystemImpl.class);
        when(fileBasedStorageSystemFactory.getStorageSystem()).thenReturn(fileBasedStorageSystem);

        storageSystemManager.deleteMedia(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE,
                TestConstants.TENANT_DOMAIN);
    }

    @Test(expectedExceptions = StorageSystemServerException.class)
    public void testIsMediaDeletedUsingIncorrectMediaStorage() throws Exception {

        mockStatic(StorageSystemUtil.class);
        when(StorageSystemUtil.getMediaStoreType()).thenReturn(TestConstants.INCORRECT_MEDIA_STORE);

        storageSystemManager.deleteMedia(TestConstants.MEDIA_UUID, TestConstants.MEDIA_TYPE, TestConstants
                .TENANT_DOMAIN);
    }

    private static void mockRealmService() {

        RealmService mockRealmService = mock(RealmService.class);
        MediaServiceDataHolder.getInstance().setRealmService(mockRealmService);
    }

    private static void startTenantFlow() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "src", "test", "resources").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(TestConstants.TENANT_ID);
    }
}

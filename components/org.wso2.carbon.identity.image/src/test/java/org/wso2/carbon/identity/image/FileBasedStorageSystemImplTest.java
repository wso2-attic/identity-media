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
import org.apache.commons.io.IOUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.image.file.FileBasedStorageSystemImpl;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({
                        IdentityTenantUtil.class, FileBasedStorageSystemImpl.class, IOUtils.class
                })
public class FileBasedStorageSystemImplTest extends PowerMockTestCase {

    private FileBasedStorageSystemImpl fileBasedStorageSystem;

    @BeforeTest
    public void setUp() {

    }

    @Test(dataProvider = "addFileDataProvider")
    public void testAddFile(String type, String uuid, int tenantId) throws Exception {

        InputStream inputStream = mock(InputStream.class);
        mockStatic(IdentityTenantUtil.class);
        when(IdentityTenantUtil.getTenantId(anyString())).thenReturn(tenantId);
        Path imagesPath = mock(Path.class);
        fileBasedStorageSystem = spy(new FileBasedStorageSystemImpl());
        doReturn(imagesPath).when(fileBasedStorageSystem, "createSpecificDirectory", anyString(), anyInt());
        Path targetLocation = mock(Path.class);
        when(imagesPath.resolve(anyString())).thenReturn(targetLocation);
        FileOutputStream fileOutputStream = mock(FileOutputStream.class);
        whenNew(FileOutputStream.class).withAnyArguments().thenReturn(fileOutputStream);
        FileChannel fileChannel = mock(FileChannel.class);
        when(fileOutputStream.getChannel()).thenReturn(fileChannel);
        mockStatic(Files.class);
        FileTime createdTime = FileTime.fromMillis(1234567890000L);
        when(Files.getAttribute((Path) anyObject(), anyString())).thenReturn(createdTime);
        String uuidHash = DigestUtils.sha256Hex(uuid + createdTime.toMillis());
        mockStatic(IOUtils.class);
        byte[] imageByteCount = new byte[10];
        when(IOUtils.toByteArray(inputStream)).thenReturn(imageByteCount);
        String url = fileBasedStorageSystem.addFile(inputStream, type, uuid, "tenantDomain");

        Assert.assertEquals(url, uuid + "_" + uuidHash + "_" + createdTime.toMillis());

    }

    @DataProvider(name = "addFileDataProvider")
    public Object[][] addFileDataProvider() {

        return new Object[][] {
                { "idp", "dacb67ee-f659-42cd-a3b2-2028924baef6", -1234 },
                { "idp", "dacb67ee-f659-42cd-a3b2-2028924baef6", 1 }
        };
    }
}

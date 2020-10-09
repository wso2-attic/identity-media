/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.media.core.internal;

import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.media.core.StorageSystemFactory;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.core.file.FileBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.core.jdbc.DatabaseBasedStorageSystemFactory;

import java.util.Dictionary;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;

@PrepareForTest(BundleContext.class)
public class MediaServiceComponentTest extends PowerMockTestCase {

    @Mock
    private ComponentContext componentContext;

    @Mock
    BundleContext bundleContext;

    @BeforeClass
    public void setUp() throws Exception {

        initMocks(this);
    }

    @Test
    public void testActivate() throws Exception {

        mockStatic(BundleContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        final String[] serviceName = new String[3];
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                for (Object obj : invocation.getArguments()) {
                    if (obj instanceof FileBasedStorageSystemFactory) {
                        FileBasedStorageSystemFactory fileBasedStorageSystemFactory = (FileBasedStorageSystemFactory)
                                invocation.getArguments()[1];
                        serviceName[0] = fileBasedStorageSystemFactory.getClass().getName();
                    } else if (obj instanceof DatabaseBasedStorageSystemFactory) {
                        DatabaseBasedStorageSystemFactory databaseBasedStorageSystemFactory =
                                (DatabaseBasedStorageSystemFactory) invocation.getArguments()[1];
                        serviceName[1] = databaseBasedStorageSystemFactory.getClass().getName();
                    }
                }
                return null;
            }
        }).when(bundleContext).registerService(anyString(), any(StorageSystemFactory.class),
                any(Dictionary.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                StorageSystemManager fileBasedStorageSystemFactory = (StorageSystemManager)
                        invocation.getArguments()[1];
                serviceName[2] = fileBasedStorageSystemFactory.getClass().getName();
                return null;
            }
        }).when(bundleContext).registerService((Class<StorageSystemManager>) anyObject(),
                any(StorageSystemManager.class), any(Dictionary.class));

        MediaServiceComponent mediaServiceComponent = new MediaServiceComponent();
        mediaServiceComponent.activate(componentContext);

        assertEquals(FileBasedStorageSystemFactory.class.getName(), serviceName[0]);
        assertEquals(DatabaseBasedStorageSystemFactory.class.getName(), serviceName[1]);
        assertEquals(StorageSystemManager.class.getName(), serviceName[2]);
    }
}

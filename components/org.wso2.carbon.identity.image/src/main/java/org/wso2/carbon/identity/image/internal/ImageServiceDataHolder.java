/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.image.internal;

import org.osgi.framework.BundleContext;
import org.wso2.carbon.identity.image.StorageSystemfactory;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

/**
 * Data holder class.
 */
public class ImageServiceDataHolder {

    private BundleContext bundleContext;
    private Map<String, StorageSystemfactory> storageSystemFactoryMap = new HashMap<>();
    private RealmService realmService;

    private ImageServiceDataHolder() {
    }

    private static class SingletonHelper {
        private static final ImageServiceDataHolder INSTANCE = new ImageServiceDataHolder();
    }

    public static ImageServiceDataHolder getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Map<String, StorageSystemfactory> getStorageSystemFactories() {

        return storageSystemFactoryMap;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

}

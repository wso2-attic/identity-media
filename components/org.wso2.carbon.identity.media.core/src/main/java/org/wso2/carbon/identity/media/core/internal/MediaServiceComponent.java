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
package org.wso2.carbon.identity.media.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.media.core.StorageSystemFactory;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.core.file.FileBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.core.jdbc.DatabaseBasedStorageSystemFactory;
import org.wso2.carbon.identity.media.core.util.StorageSystemUtil;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Service component class responsible for registering media service factories and exporting the osgi service.
 */
@Component(name = "MediaServiceComponent",
           immediate = true)
public class MediaServiceComponent {

    private static final Log LOGGER = LogFactory.getLog(MediaServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        MediaServiceDataHolder.getInstance().setBundleContext(bundleContext);
        StorageSystemFactory fileBasedStorageSystemFactory = new FileBasedStorageSystemFactory();
        bundleContext.registerService(StorageSystemFactory.class.getName(), fileBasedStorageSystemFactory, null);
        StorageSystemFactory dbBasedStorageSystemFactory = new DatabaseBasedStorageSystemFactory();
        bundleContext.registerService(StorageSystemFactory.class.getName(), dbBasedStorageSystemFactory, null);
        bundleContext.registerService(StorageSystemManager.class, new StorageSystemManager(), null);

        // Load configurations from media.properties file into memory.
        StorageSystemUtil.loadMediaProperties();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Media service bundle deactivated.");
        }
    }

    @Reference(name = "MediaServiceComponent",
               service = StorageSystemFactory.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unSetStorageSystemFactory")
    protected void setStorageSystemFactory(StorageSystemFactory storageSystemFactory) {

        MediaServiceDataHolder.getInstance().getStorageSystemFactories()
                .put(storageSystemFactory.getStorageType(), storageSystemFactory);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added storage system of type " + storageSystemFactory.getStorageType());
        }
    }

    protected void unSetStorageSystemFactory(StorageSystemFactory storageSystemfactory) {

        MediaServiceDataHolder.getInstance().getStorageSystemFactories().remove(storageSystemfactory.getStorageType());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Removed storage system of type " + storageSystemfactory.getStorageType());
        }
    }

    @Reference(name = "RealmService",
               service = org.wso2.carbon.user.core.service.RealmService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unSetRealmService")
    protected void setRealmService(RealmService realmService) {

        MediaServiceDataHolder.getInstance().setRealmService(realmService);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RealmService is set in the media service bundle.");
        }
    }

    protected void unSetRealmService(RealmService realmService) {

        MediaServiceDataHolder.getInstance().setRealmService(null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("RealmService is removed from the media service bundle.");
        }
    }
}

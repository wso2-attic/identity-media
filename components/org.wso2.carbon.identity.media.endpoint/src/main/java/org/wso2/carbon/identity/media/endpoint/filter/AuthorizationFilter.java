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

package org.wso2.carbon.identity.media.endpoint.filter;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.auth.service.AuthenticationContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.core.exception.StorageSystemException;
import org.wso2.carbon.identity.media.core.internal.MediaServiceDataHolder;
import org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.CONTENT_PATH_COMPONENT;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.ME_PATH_COMPONENT;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.PUBLIC_PATH_COMPONENT;
import static org.wso2.carbon.identity.media.endpoint.common.Util.getStorageSystemManager;
import static org.wso2.carbon.identity.media.endpoint.common.Util.getTenantDomainFromContext;
import static org.wso2.carbon.identity.media.endpoint.common.Util.handleException;

/**
 * Filter to evaluate access level security for media download requests and media management requests.
 */
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final Log LOG = LogFactory.getLog(AuthorizationFilter.class);
    private static final String AUTH_CONTEXT = "auth-context";
    private static final String VIEW_PERMISSION = "permission/admin/manage/identity/media/view";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

        evaluateDownloadSecurityForMedia(containerRequestContext);
        evaluateMediaManagementSecurityForEndUser(containerRequestContext);
    }

    private void evaluateDownloadSecurityForMedia(ContainerRequestContext containerRequestContext) {

        if (containerRequestContext.getMethod().equals(HTTPConstants.HTTP_METHOD_GET)) {

            List<PathSegment> pathSegments = containerRequestContext.getUriInfo().getPathSegments();
            String accessLevel = pathSegments.get(0).getPath();
            String type = pathSegments.get(1).getPath();
            String uuid = pathSegments.get(2).getPath();

            AuthorizationManager authorizationManager = null;
            StorageSystemManager storageSystemManager = getStorageSystemManager();
            String tenantDomain = getTenantDomainFromContext();
            boolean isUserAuthorized = false;
            try {
                if (accessLevel.equals(PUBLIC_PATH_COMPONENT)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Evaluating whether media download is publicly allowed.");
                    }
                    isUserAuthorized = storageSystemManager.isDownloadAllowedForPublicMedia(uuid, type, tenantDomain);
                    if (!isUserAuthorized) {
                        Response response = Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                        containerRequestContext.abortWith(response);
                    }
                } else if (accessLevel.equals(CONTENT_PATH_COMPONENT)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Evaluating access based security for protected media download request.");
                    }
                    AuthenticationContext authenticationContext = (AuthenticationContext) containerRequestContext
                            .getProperty(AUTH_CONTEXT);
                    User user = null;
                    if (authenticationContext != null) {
                        user = ((AuthenticationContext) containerRequestContext.getProperty(AUTH_CONTEXT)).getUser();
                    }
                    if (user != null) {
                        RealmService realmService = MediaServiceDataHolder.getInstance().getRealmService();
                        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
                        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
                        if (tenantUserRealm != null) {
                            authorizationManager = tenantUserRealm.getAuthorizationManager();
                        }
                        if (authorizationManager != null) {
                            isUserAuthorized = authorizationManager.isUserAuthorized(UserCoreUtil.addDomainToName(
                                    user.getUserName(), user.getUserStoreDomain()), VIEW_PERMISSION,
                                    CarbonConstants.UI_PERMISSION_ACTION);
                        }
                    }

                    if (!isUserAuthorized) {
                        isUserAuthorized = storageSystemManager.isDownloadAllowedForProtectedMedia(uuid, type,
                                tenantDomain);
                    }

                    if (!isUserAuthorized) {
                        Response response = Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                        containerRequestContext.abortWith(response);
                    }
                }
            } catch (UserStoreException | StorageSystemException e) {
                MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                        ERROR_CODE_ERROR_EVALUATING_ACCESS_SECURITY;
                Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
                throw handleException(e, errorMessage, LOG, status, uuid);
            }
        }
    }

    private void evaluateMediaManagementSecurityForEndUser(ContainerRequestContext containerRequestContext) {

        String httpMethod = containerRequestContext.getMethod();
        if (httpMethod.equals(HTTPConstants.HEADER_DELETE) || httpMethod.equals(HTTPConstants.HEADER_GET)) {

            List<PathSegment> pathSegments = containerRequestContext.getUriInfo().getPathSegments();
            String accessLevel = pathSegments.get(0).getPath();
            String type = pathSegments.get(1).getPath();
            String uuid = pathSegments.get(2).getPath();
            String tenantDomain = getTenantDomainFromContext();

            StorageSystemManager storageSystemManager = getStorageSystemManager();
            boolean isUserAuthorized;
            try {
                if (accessLevel.equals(ME_PATH_COMPONENT)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Evaluating media management security of an end-user for %s request.",
                                httpMethod));
                    }
                    isUserAuthorized = storageSystemManager.isMediaManagementAllowedForEndUser(uuid, type,
                            tenantDomain);
                    if (!isUserAuthorized) {
                        Response response = Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
                        containerRequestContext.abortWith(response);
                    }
                }
            } catch (StorageSystemException e) {
                MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                        ERROR_CODE_ERROR_EVALUATING_MEDIA_MANAGEMENT_SECURITY;
                Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
                throw handleException(e, errorMessage, LOG, status, uuid);
            }
        }
    }

}

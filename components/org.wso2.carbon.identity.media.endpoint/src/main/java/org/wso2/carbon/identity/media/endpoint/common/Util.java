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

package org.wso2.carbon.identity.media.endpoint.common;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.media.core.StorageSystemManager;
import org.wso2.carbon.identity.media.endpoint.Error;
import org.wso2.carbon.identity.media.endpoint.exception.MediaEndpointException;

import java.net.URI;
import java.util.UUID;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.MEDIA_API_PATH_COMPONENT;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.media.endpoint.common.MediaServiceConstants.V1_API_PATH_COMPONENT;

/**
 * This class includes the utility methods for media endpoint.
 */
public class Util {

    private static final Log LOG = LogFactory.getLog(Util.class);

    private Util() {

    }

    /**
     * Get the storage system manager.
     *
     * @return StorageSystemManager
     */
    public static StorageSystemManager getStorageSystemManager() {

        return (StorageSystemManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(StorageSystemManager.class, null);
    }

    /**
     * Handle exceptions generated in the media endpoint.
     *
     * @param status       HTTP Status.
     * @param errorMessage Error message information.
     * @param data         Error data.
     * @return MediaEndpointException
     */
    public static MediaEndpointException handleException(Response.Status status,
                                                         MediaServiceConstants.ErrorMessage errorMessage,
                                                         String... data) {

        return new MediaEndpointException(status, getError(errorMessage, data));
    }

    /**
     * Handle exceptions generated in the media endpoint.
     *
     * @param e            Exception caught.
     * @param errorMessage Error message information.
     * @param log          Logger.
     * @param status       HTTP Status.
     * @param data         Error data.
     * @return MediaEndpointException
     */
    public static MediaEndpointException handleException(Exception e, MediaServiceConstants.ErrorMessage errorMessage,
                                                         Log log, Response.Status status, String... data) {

        Error error = buildError(errorMessage, log, e, errorMessage.getDescription(), data);
        return new MediaEndpointException(status, error);
    }

    /**
     * Returns a generic error object.
     *
     * @param errorMessage Error message information.
     * @param data         Error data.
     * @return A generic error with the specified details.
     */
    public static Error getError(MediaServiceConstants.ErrorMessage errorMessage, String... data) {

        Error error = new Error();
        error.setCode(errorMessage.getCode());
        error.setDescription(buildErrorDescription(errorMessage, data));
        error.setMessage(errorMessage.getMessage());
        error.setTraceId(getCorrelation());
        return error;
    }

    private static String buildErrorDescription(MediaServiceConstants.ErrorMessage errorMessage, String... data) {

        String errorDescription;
        if (ArrayUtils.isNotEmpty(data)) {
            errorDescription = String.format(errorMessage.getDescription(), data);
        } else {
            errorDescription = errorMessage.getDescription();
        }
        return errorDescription;
    }

    private static Error buildError(MediaServiceConstants.ErrorMessage errorMessage, Log log, Exception e,
                                    String message, String... data) {

        Error error = getError(errorMessage, data);
        String errorMessageFormat = "errorCode: %s | message: %s";
        String errorMsg = String.format(errorMessageFormat, error.getCode(), message);
        log.error(errorMsg, e);
        return error;
    }

    /**
     * Get correlation id of current thread.
     *
     * @return correlation-id
     */
    private static String getCorrelation() {

        String ref;
        if (isCorrelationIDPresent()) {
            ref = MDC.get(CORRELATION_ID_MDC).toString();
        } else {
            ref = UUID.randomUUID().toString();
        }
        return ref;
    }

    /**
     * Check whether correlation id present in the log MDC.
     *
     * @return whether the correlation id is present.
     */
    private static boolean isCorrelationIDPresent() {

        return MDC.get(CORRELATION_ID_MDC) != null;
    }

    /**
     * Retrieves loaded tenant domain from carbon context.
     *
     * @return tenant domain of the request is being served.
     */
    public static String getTenantDomainFromContext() {

        String tenantDomain = IdentityTenantUtil.getTenantDomainFromContext();
        if (StringUtils.isBlank(tenantDomain)) {
            tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        }
        return tenantDomain;
    }

    /**
     * Retrieves authenticated username from carbon context.
     *
     * @return username of the authenticated user.
     */
    public static String getUsernameFromContext() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    /**
     * Get location for uploaded media.
     *
     * @param id          The unique identifier for uploaded media.
     * @param type        The high level content-type of the resource (if media content-type is image/png then
     *                    type would be image).
     * @param accessLevel Media download access level.
     * @return URI
     */
    public static URI getResourceLocation(String id, String type, String accessLevel) {

        return buildURIForHeader(String.format(V1_API_PATH_COMPONENT + "/%s/%s/%s",
                accessLevel, type, id));
    }

    private static URI buildURIForHeader(String endpoint) {

        URI loc;
        String context = getContext(endpoint);

        try {
            String url = ServiceURLBuilder.create().addPath(context).build().getAbsolutePublicURL();
            loc = URI.create(url);
        } catch (URLBuilderException e) {
            MediaServiceConstants.ErrorMessage errorMessage = MediaServiceConstants.ErrorMessage.
                    ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL;
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            throw handleException(e, errorMessage, LOG, status);
        }
        return loc;
    }

    /**
     * Builds the API context on whether the tenant qualified url is enabled or not. In tenant qualified mode the
     * ServiceURLBuilder appends the tenant domain to the URI as a path param automatically. But
     * in non tenant qualified mode we need to append the tenant domain to the path manually.
     *
     * @param endpoint Relative endpoint path.
     * @return Context of the API.
     */
    private static String getContext(String endpoint) {

        String context;
        if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
            context = MEDIA_API_PATH_COMPONENT + endpoint;
        } else {
            context = String.format(TENANT_CONTEXT_PATH_COMPONENT, getTenantDomainFromContext()) +
                    MEDIA_API_PATH_COMPONENT + endpoint;
        }
        return context;
    }

}

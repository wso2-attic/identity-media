# Configuring Media Service

## Building the artifacts from source

1. Get a clone from https://github.com/wso2/identity-media.git or download the source.
2. Run the below maven command from identity-media directory,
- `mvn clean install`

## Deploying media service artifacts

1. Place the `org.wso2.carbon.identity.media.core-2.0.x.jar` file available in `identity-media/components/org.wso2.carbon.identity.media.core/target/` directory into the `<IS_HOME>/repository/components/dropins` directory.
2. Place the `api#identity#media#v1.0.war` file available in `identity-media/components/org.wso2.carbon.identity.media.endpoint/target` directory into the `<IS_HOME>/repository/deployment/server/webapps` directory.

## Overriding default media configurations

The default media service related configurations are found at https://github.com/wso2/identity-media/blob/master/components/org.wso2.carbon.identity.media.core/src/main/resources/META-INF/media.properties

These configurations can be overridden via the following environment variables.

- To configure maximum allowed file upload size in bytes: `MEDIA_MAX_BYTE_SIZE`
- The default file mount location for the uploaded media is configured with the base directory as `<IS_HOME>/repository`. To override this location: `MEDIA_MOUNT_LOCATION`
- To configure media storage type: `MEDIA_STORE_TYPE`
- To configure allowed high level file content types: `MEDIA_CONTENT_TYPES`
   - Note: content types should be comma separated
       - eg: `export MEDIA_CONTENT_TYPES=image,text`
- To configure allowed sub types for a given high level file content type: `MEDIA_$contentType_CONTENT_SUB_TYPES` where $contentType is the high level content type. 
   - Note: content sub types should be comma separated
       - For example to define the sub types for image content type: `export MEDIA_IMAGE_CONTENT_SUB_TYPES=jpeg,png,svg+xml,tiff,webp`)

## Additional configurations

In the configured media mount base location it is required to manually create a directory called `media`. The uploaded files will be saved in this `media` folder.

## Try it out

Refer the API definition: https://github.com/wso2/identity-media/blob/master/components/org.wso2.carbon.identity.media.endpoint/src/main/resources/media_endpoint.yaml

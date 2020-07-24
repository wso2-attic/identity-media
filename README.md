# identity-media

This is the media service for WSO2 Identity Server.

The media service offers the following capabilities:
- Upload media
- Download media
- Delete media
- View media information

Media service has been implemented to provide the flexibility to configure the preferred storage system for media persistence. Currently, only file-based media storing is supported. Whenever a media is uploaded a unique id will be generated and returned in the response of media upload request. This unique id should be used for media downloading and other media management operations such as deleting media and viewing media information.

In addition to the aforementioned functionality, this service gives an extension to transform the input stream. According to the requirement, the media can be transformed. Ex:- The image size can be reduced using this extension.

Refer [Configuring Media Service](docs/README.md)
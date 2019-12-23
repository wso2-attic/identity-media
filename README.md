# identity-content-repository
This is the identity content provider service. This service can be used to upload, download and delete the images of service provider(application), identity provider and user.

Images can be stored as file or stored through any StorageSystem. Currently only file based image storing has been supported. Whenever a file is uploaded a unique id will be populated and returned in the response of uploadFile request. The uniqueid return through the uploadFile response should be used to download the file.

Inaddition to the above mentioned functionality this service gives an extension to transform the input stream. According to the requirement the image can be transformed. Ex:- The image size can be reduced using this extension.

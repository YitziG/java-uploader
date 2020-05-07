package org.yitzi.video.api.resource;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.yitzi.video.api.domain.UploadToAPIVideo;
import org.yitzi.video.core.EnvironmentProperties;
import org.yitzi.video.core.access.VideoAccess;
import org.yitzi.video.core.util.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/videos")
public class VideoUploaderResource {

    UploadToAPIVideo uploadToAPIVideo = new UploadToAPIVideo();

    @POST
    @Path("/{uniqueURL}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void uploadVideo(@FormDataParam("file") InputStream file, @FormDataParam("file") FormDataBodyPart fileMeta,
                            @FormDataParam("file") FormDataContentDisposition fileDetail, @PathParam("uniqueURL") String uniqueURL) {
        uploadToAPIVideo.UploadToAPIVideo(uniqueURL);
    }

    @GET
    @Path("/uploader")
    public String getUploadURL(@QueryParam("api_key") String apiKey, @QueryParam("tag") String tag) {
        String url = StringUtils.generateUniqueString();
        VideoAccess videoAccess = VideoAccess.getInstance();
        int adminID = videoAccess.upsertAdmin(apiKey);
        int placeHolderID = videoAccess.insertVideoPlaceHolder(url, tag);
        videoAccess.insertAdminVideoRelationship(adminID, placeHolderID);
        return EnvironmentProperties.getProperty("web_url") + "/upload/" + url;
    }
}

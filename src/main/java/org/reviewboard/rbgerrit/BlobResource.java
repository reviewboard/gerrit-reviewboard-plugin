package org.reviewboard.rbgerrit;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.TypeLiteral;
import org.eclipse.jgit.lib.ObjectId;


/**
 * The blob information resource.
 *
 * This resource has child resources responsible for returning blob information
 * ({@link GetBlobInfo}) and its content ({@link GetBlobContent}).
 *
 * This resource will only be instantiated when a blob with the given
 * {@link ObjectId} exists in the repository. Cases where it doesn't exist
 * (or is a different object type) are caught in the parent collection:
 * {@link BlobsCollection}.
 */
public class BlobResource implements RestResource {
    /**
     * A constant that signifies this resource.
     *
     * Other resources can use this to bind themselves as child resources.
     * */
    public static final TypeLiteral<RestView<BlobResource>> BLOB_KIND = new TypeLiteral<RestView<BlobResource>>() {};

    private final ProjectResource projectResource;
    private final ObjectId objectId;

    /**
     * Construct a BlobResource.
     * @param projectResource The parent project resource.
     * @param objectId The object ID of the file.
     */
    BlobResource(final ProjectResource projectResource, final ObjectId objectId) {
        this.projectResource = projectResource;
        this.objectId = objectId;
    }

    /**
     * Return the project's access control management.
     * @return The project's access control management.
     */
    public ProjectControl getProjectControl() {
        return projectResource.getControl();
    }

    /**
     * Return the object ID.
     * @return The object ID.
     */
    public ObjectId getObjectId() {
        return objectId;
    }
}

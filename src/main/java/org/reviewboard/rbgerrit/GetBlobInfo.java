package org.reviewboard.rbgerrit;

import com.google.gerrit.extensions.restapi.RestReadView;


/**
 * An endpoint for determining if a blob exists in the repository.
 */
public class GetBlobInfo implements RestReadView<BlobResource> {
    /**
     * Return information about a blob ID.
     *
     * This endpoint is always successful because if the blob in question
     * doesn't exist, then the parent resource returns a 404.
     * @param parentResource The parent file resource.
     * @return Information about the object ID.
     */
    @Override
    public BlobInfo apply(final BlobResource parentResource) {
        return new BlobInfo(parentResource);
    }

    /**
     * Information about a blob.
     */
    public static class BlobInfo {
        public final String blobId;

        /**
         * Construct a new BlobInfo.
         * @param parentResource The parent resource, containing the object
         *                       ID of the blob in question.
         */
        public BlobInfo(final BlobResource parentResource) {
            blobId = parentResource.getObjectId().getName();
        }
    }

}

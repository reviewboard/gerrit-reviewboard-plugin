package org.reviewboard.rbgerrit;

import static com.google.gerrit.server.project.CommitResource.COMMIT_KIND;
import static com.google.gerrit.server.project.BranchResource.PROJECT_KIND;
import static org.reviewboard.rbgerrit.BlobResource.BLOB_KIND;

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.inject.AbstractModule;


/**
 * The rbgerrit module.
 *
 * This module is responsible for installing our extensions to Gerrit's REST API.
 */
public class Module extends AbstractModule {
    /**
     * Install our extensions to Gerrit's REST API.
     */
    @Override
    protected void configure() {
        install(new RestApiModule() {
            @Override
            protected void configure() {
                get(COMMIT_KIND, "diff").to(DiffResource.class);
                get(PROJECT_KIND, "all-commits").to(CommitListResource.class);
                child(PROJECT_KIND, "blobs").to(BlobsCollection.class);

                DynamicMap.mapOf(binder(), BLOB_KIND);
                get(BLOB_KIND, "/").to(GetBlobInfo.class);
                get(BLOB_KIND, "content").to(GetBlobContent.class);
            }
        });
    }
}

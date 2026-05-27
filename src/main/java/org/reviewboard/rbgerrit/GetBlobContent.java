package org.reviewboard.rbgerrit;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;


/**
 * A resource that returns the content of a file.
 */
public class GetBlobContent implements RestReadView<BlobResource> {
    final static Logger log = LoggerFactory.getLogger(GetBlobContent.class);
    private final GitRepositoryManager repoManager;

    /* An upper bound on cached file sizes.
     *
     * We try to retrieve files that are cached in memory. Any such cached file
     * will be smaller than this value, in which case we have to read from the
     * repository.
     *
     * From FileContentUtil.java.
     */
    private static final int MAX_CACHED_SIZE = 5 << 20;

    /**
     * Contruct a new resource.
     * @param repoManager The repository manager.
     */
    @Inject
    public GetBlobContent(final GitRepositoryManager repoManager) {
        this.repoManager = repoManager;
    }

    /**
     * Return the content of the given blob as base64-encoded data.
     * @param parentResource The parent BlobResource, containing the project
     *                       information and blob ID.
     * @return The content of the blob as base64-encoded data.
     * @throws RestApiException Raised if an IO error occurs while reading
     *                          the blob content.
     */
    @Override
    public BinaryResult apply(final BlobResource parentResource) throws RestApiException {
        final Project.NameKey projectName = parentResource.getProjectNameKey();
        try (final Repository repository = repoManager.openRepository(projectName)) {
            final ObjectLoader loader = repository.open(parentResource.getObjectId(), OBJ_BLOB);
            BinaryResult result;

            try {
                final byte[] bytes = loader.getCachedBytes(MAX_CACHED_SIZE);
                result = BinaryResult.create(bytes);
            } catch (final LargeObjectException e) {
                result = new BinaryResult() {
                    @Override
                    public void writeTo(OutputStream stream) throws IOException {
                        loader.copyTo(stream);
                    }
                };
            }

            return result
                .setContentLength(loader.getSize())
                .setContentType("application/octet-stream")
                .base64();
        } catch (final IOException e) {
            log.error(String.format("Error reading git repository for project '%s': %s", projectName.get(), e), e);

            throw new RestApiException("Could not read repository: " + e.toString());
        }
    }
}

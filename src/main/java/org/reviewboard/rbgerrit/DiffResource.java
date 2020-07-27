package org.reviewboard.rbgerrit;

import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.CommitResource;
import com.google.inject.Inject;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * A resource for computing diffs of commits.
 */
public class DiffResource implements RestReadView<CommitResource> {
    private static final String DIFF_MIMETYPE = "text/x-patch";
    private static final Logger log = LoggerFactory.getLogger(DiffResource.class);

    private final GitRepositoryManager repoManager;

    /**
     * Construct the DiffResource.
     * @param manager The git repository manager.
     */
    @Inject
    public DiffResource(final GitRepositoryManager manager) {
        repoManager = manager;
    }

    /**
     * Return the diff of the given commit.
     * @param parentResource The parent commit resource.
     * @return The diff of the given commit.
     * @throws RestApiException If an invalid commit (i.e., one with 0 or 2+
     *                          parents) is given.
     */
    @Override
    public BinaryResult apply(final CommitResource parentResource) throws RestApiException {
        final Project.NameKey projectName = parentResource.getProjectState().getProject().getNameKey();
        final RevCommit commit = parentResource.getCommit();
        final int parentCount = commit.getParentCount();

        if (parentCount == 0) {
            throw new RestApiException("Cannot retrieve diff of commits with 0 parents.");
        } else if (parentCount > 1) {
            throw new RestApiException("Cannot retrieve diff of commit with multiple parents.");
        }

        try (final Repository repository = repoManager.openRepository(projectName)) {
            return BinaryResult
                .create(diff(repository, commit))
                .setContentType(DIFF_MIMETYPE);
        } catch (final IOException e) {
            log.error(String.format("Error reading git repository for project '%s': %s", projectName.get(), e), e);

            throw new RestApiException(String.format("Could not retrieve diff: %s", e.toString()));
        }
    }

    /**
     * Return the diff of a commit that has a single parent.
     * @param repo The repository containing the commit.
     * @param commit The commit for which the diff will be computed.
     * @return The diff.
     * @throws IOException If an error occurs while attempting to read the Git
     *                     object store.
     */
    private byte[] diff(final Repository repo, final RevCommit commit) throws IOException {
        assert commit.getParentCount() == 0;

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final RevCommit parent = commit.getParent(0);

        final DiffFormatter fmt = new DiffFormatter(output);
        fmt.setRepository(repo);
        fmt.format(parent.getTree(), commit.getTree());
        return output.toByteArray();
    }
}

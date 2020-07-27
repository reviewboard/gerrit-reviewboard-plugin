package org.reviewboard.rbgerrit;

import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.MaxCountRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


/**
 * A resource that returns a list of commits on a branch.
 */
public class CommitListResource implements RestReadView<ProjectResource> {
    private static final int MAX_RESULTS_PER_PAGE = 30;
    private static final Logger log = LoggerFactory.getLogger(CommitListResource.class);

    private final GitRepositoryManager repoManager;
    private String start = "master";
    private int limit = MAX_RESULTS_PER_PAGE;

    /**
     * Set the starting revision.
     *
     * This defaults to the commit ID of the current branch.
     *
     * @param start The commit ID to start at.
     */
    @Option(name="--start", metaVar="REF", usage="Revision to start at.")
    public void setStart(final String start) { this.start = start; }

    /**
     * Set the limit of commmits to return.
     *
     * This defaults to 30.
     *
     * @param limit The new limit. If this is larger than 30, it will be set to
     *              30.
     */
    @Option(name="--limit", metaVar="COUNT", usage="Limit results to CNT commits.")
    public void setLimit(final int limit) {
        this.limit = Math.min(MAX_RESULTS_PER_PAGE, limit);
    }

    /**
     * Construct the CommitListResource.
     * @param manager The git repository manager.
     */
    @Inject
    public CommitListResource(final GitRepositoryManager manager) {
        repoManager = manager;
    }

    /**
     * Return a list of commits from the given starting point.
     *
     * If no starting point is specified, it will start from the branch HEAD.
     * @param parentResource The parent resource.
     * @return The list of commits.
     * @throws RestApiException If an error occurs reading the Git repository.
     */
    @Override
    public Response<Collection<CommitInfo>> apply(final ProjectResource parentResource) throws RestApiException {
        final Project.NameKey projectName = parentResource.getNameKey();

        try (final Repository repository = repoManager.openRepository(projectName)) {
            final ObjectId startId = repository.resolve(String.format("%s^{commit}", start));
            final RevWalk walk = new RevWalk(repository);
            final List<CommitInfo> commits = new ArrayList<>(this.limit);

            walk.setRevFilter(AndRevFilter.create(new NoMergeRevFilter(), MaxCountRevFilter.create(this.limit)));
            walk.markStart(walk.parseCommit(startId));
            walk.forEach(c -> commits.add(new CommitInfo(c)));

            return Response.ok(commits);
        } catch (final AmbiguousObjectException | IncorrectObjectTypeException e) {
            throw new ResourceNotFoundException();
        } catch (final IOException e) {
            log.error(String.format("Error reading git repository for project '%s': %s", projectName.get(), e), e);

            throw new RestApiException("Error reading repository: " + e.toString());
        }
    }

    /**
     * Information about a single commit.
     */
    public static class CommitInfo {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        public final String message;
        public final String revision;
        public final String author;
        public final String[] parents;
        public final String time;

        /**
         * Construct the CommitInfo object.
         * @param commit The commit to retrieve information from.
         */
        CommitInfo(final RevCommit commit) {
            assert commit.getParentCount() == 1;

            final PersonIdent ident = commit.getAuthorIdent();
            ZonedDateTime zdt =  ZonedDateTime.ofInstant(ident.getWhen().toInstant(),ident.getTimeZone().toZoneId());
            time = formatter.format(zdt);
            message = commit.getFullMessage();
            revision = commit.getId().getName();
            author = ident.getName();
            parents = new String[]{ commit.getParent(0).getId().getName() };
        }
    }

    /**
     * A RevWalk filter that excludes merge commits.
     */
    private static class NoMergeRevFilter extends RevFilter {
        /**
         * Return whether or not to include a commit in the walk.
         *
         * The commit will only be included if it is not a merge commit (i.e.,
         * it has only a single parent).
         *
         * @param walk The current walk.
         * @param commit The commit to include or exclude.
         * @return Whether or not to include the given commit.
         */
        @Override
        public boolean include(final RevWalk walk, final RevCommit commit) {
            return commit.getParentCount() == 1;
        }

        /**
         * Return a copy of this filter.
         * @return A copy of this filter.
         */
        @Override
        public RevFilter clone() {
            return this;
        }
    }
}

package org.reviewboard.rbgerrit;


import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.InvalidObjectIdException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * A collection that maps a blob ID into a {@link BlobResource}.
 *
 * This class ensures that a {@link BlobResource} will only ever be instantiated when the given blob ID maps to a valid
 * blob in the repository. Otherwise, a 404 error will be returned.
 */
@Singleton
public class BlobsCollection implements ChildCollection<ProjectResource, BlobResource> {
    private final static Logger log = LoggerFactory.getLogger(BlobsCollection.class);

    private final DynamicMap<RestView<BlobResource>> views;
    private final GitRepositoryManager repoManager;

    /**
     * Construct a new BlobsCollection
     * @param views The child views of this collection.
     * @param repoManager The repository manager.
     */
    @Inject
    public BlobsCollection(final DynamicMap<RestView<BlobResource>> views, final GitRepositoryManager repoManager) {
        this.views = views;
        this.repoManager = repoManager;
    }

    /**
     * Parse a blob ID into a BlobResource.
     * @param parentResource The parent ProjectResource.
     * @param id The url fragment representing the blob ID.
     * @return The child resource.
     * @throws RestApiException Thrown when an error occurs reading the repository or if the given ID is invalid for any
     *                          reason.
     */
    @Override
    public BlobResource parse(final ProjectResource parentResource, final IdString id) throws RestApiException {
        final Project.NameKey projectName = parentResource.getProjectState().getNameKey();;

        try (final Repository repository = repoManager.openRepository(projectName)) {
            final ObjectId objId = repository.resolve(String.format("%s^{blob}", id.get()));

            if (objId == null) {
                throw new MissingObjectException(ObjectId.fromString(id.get()), "blob");
            }

            return new BlobResource(parentResource, objId);
        } catch (final MissingObjectException | IncorrectObjectTypeException | InvalidObjectIdException e) {
            throw new ResourceNotFoundException(String.format(
                "No blob with id '%s' found in the repository.'",
                id.get()
            ));
        } catch (final IOException e) {
            log.error(String.format("Error reading git repository for project '%s': %s", projectName.get(), e), e);

            throw new RestApiException("Error reading repository: " + e.toString());
        }
    }

    /**
     * Do not return a list of all members of the collection.
     *
     * If implemented, this would return each file blob sequentially, which is not something we want to expose. Hence
     * it will cause a 404.
     * @return Nothing.
     * @throws ResourceNotFoundException
     */
    @Override
    public RestView<ProjectResource> list() throws ResourceNotFoundException {
        throw new ResourceNotFoundException();
    }


    /**
     * Return the child views of this view.
     * @return The child views.
     */
    @Override
    public DynamicMap<RestView<BlobResource>> views() {
        return views;
    }

}

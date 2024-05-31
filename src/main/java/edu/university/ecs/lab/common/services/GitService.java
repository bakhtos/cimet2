package edu.university.ecs.lab.common.services;

import edu.university.ecs.lab.common.config.ConfigUtil;
import edu.university.ecs.lab.common.config.Config;
import edu.university.ecs.lab.common.error.Error;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;


import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing local repositories including the cloning
 * and resetting the current commit.
 */
public class GitService {
  private static final int EXIT_SUCCESS = 0;
  private static final String HEAD_COMMIT = "HEAD";


  /**
   * Configuration file path
   */
  private final Config config;

  private final Repository repository;

  public GitService(String configPath) {
    this.config = ConfigUtil.readConfig(configPath);
    validateLocalExists();
    this.repository = initRepository();

  }

  /**
   * This method clones a remote repository to the local file system. Postcondition: the repository
   * has been cloned to the local file system.
   *
   * @throws Exception if Git clone failed
   */
  public void cloneRemote() {
    try {
      ProcessBuilder processBuilder =
              new ProcessBuilder("git", "clone", config.getRepositoryURL(), config.getLocalClonePath());
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      int exitCode = process.waitFor();


      if (exitCode == EXIT_SUCCESS) {
        // If clone was successful we can now reset local repo to config base commit
        resetLocal(config.getBaseCommit());

      } else {
        throw new Exception();
      }

    } catch (Exception e) {
      Error.reportAndExit(Error.GIT_FAILED);
    }
  }

  /**
   * This method resets the local repository to commitID.
   * Used to initially set commit for clone and additionally to
   * advance the local repository as we step through commits
   *
   * @param commitID if empty or null, defaults to HEAD
   */
  public void resetLocal(String commitID)  {
    if(Objects.isNull(commitID) || commitID.isEmpty()) {
      commitID = HEAD_COMMIT;
    }

    try (Git git = new Git(repository)) {
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commitID).call();
    } catch (Exception e) {
      Error.reportAndExit(Error.GIT_FAILED);
    }

  }

  /**
   * This method reset's the local branch to a relative commit
   * from head
   *
   * @param relativeIndex if empty or null, defaults to HEAD
   * @return boolean indicating success
   */
  public boolean resetLocal(int relativeIndex) {

    try (Git git = new Git(repository)) {
      String relativeHead = HEAD_COMMIT + "~" + relativeIndex;
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef(relativeHead).call();
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  /**
   * This method validates that the local repository exists or
   * reports and exits if it doesn't.
   */
  private void validateLocalExists() {
    File file = new File(config.getLocalClonePath());
    if(!(file.exists() && file.isDirectory())) {
      Error.reportAndExit(Error.REPO_DONT_EXIST);
    }
  }

  /**
   * Establish a local endpoint for the given repository path.
   *
   * @return the repository object
   */
  public Repository initRepository() {
    Repository repository = null;

    try {
      File localRepoDir = new File(config.getLocalClonePath());
      repository = new FileRepositoryBuilder().setGitDir(new File(localRepoDir, ".git")).build();

    } catch (Exception e) {
      Error.reportAndExit(Error.GIT_FAILED);
    }

    return repository;
  }

  /**
   * Get the differences between local branch defined by config and branch + 1.
   *
   * @param relativeIndex the relative index from head that we will compare head to
   * @return the list of differences
   */
  public List<DiffEntry> getDifferences(int relativeIndex) throws Exception {
    List<DiffEntry> returnList = null;

    RevWalk revWalk = new RevWalk(repository);
    String relativeHead = HEAD_COMMIT + "~" + relativeIndex;
    RevCommit currentCommit = revWalk.parseCommit(repository.resolve(HEAD_COMMIT));
    RevCommit parentCommit = revWalk.parseCommit(repository.resolve(relativeHead));

    // Prepare tree parsers for both commits
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
      CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
      oldTreeParser.reset(reader, parentCommit.getTree());
      newTreeParser.reset(reader, currentCommit.getTree());

      // Compute differences
      try (Git git = new Git(repository)) {
        returnList = git.diff()
                .setNewTree(newTreeParser)
                .setOldTree(oldTreeParser)
                .call();

      }
    }

    return returnList;
  }


}

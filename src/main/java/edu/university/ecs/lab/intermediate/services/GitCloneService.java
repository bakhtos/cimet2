package edu.university.ecs.lab.intermediate.services;

import edu.university.ecs.lab.common.config.InputRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Service for cloning remote repositories to the local file system. */
@Data
@AllArgsConstructor
public class GitCloneService {
  /** The absolute root path to clone the repositories to */
  private final String clonePath;

  /**
   * This method clones remote repositories to the local file system
   *
   * @param inputRepositories the repositories to be cloned
   * @throws Exception if Git clone failed
   */
  public List<String> cloneRemotes(InputRepository[] inputRepositories) throws Exception {
    List<String> repoNames = new ArrayList<>();

    for (InputRepository repo : inputRepositories) {
      String output = System.getProperty("user.dir") + clonePath + File.separator + getRepositoryName(repo.getRepoUrl());
      ProcessBuilder processBuilder = new ProcessBuilder("git", "clone", repo.getRepoUrl(), output);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      int exitCode = process.waitFor();

      if (exitCode < 400) {
        System.out.println("Git clone of " + repo.getRepoUrl() + " successful ");

        if (repo.getBaseCommit() != null && !repo.getBaseCommit().isEmpty()) {
          processBuilder = new ProcessBuilder("git", "reset", "--hard", repo.getBaseCommit());
          processBuilder.directory(new File(output));
          processBuilder.redirectErrorStream(true);
          process = processBuilder.start();

          exitCode = process.waitFor();

          // TODO exit code not working
          if (exitCode < 400) {
            System.out.println("Git reset of " + repo.getRepoUrl() + " successful ");
          } else {
            throw new Exception(
                "Git reset of " + repo.getRepoUrl() + " failed with status code: " + exitCode);
          }
        }
      } else {
        throw new Exception(
            "Git clone of " + repo.getRepoUrl() + " failed with status code: " + exitCode);
      }

      output = output.replaceAll("\\\\", "/");

      // add microservices to path
      if (repo.getPaths() != null && repo.getPaths().length > 0) {
        for (String subPath : repo.getPaths()) {
          String path;
          if (subPath.substring(0, 1).equals(File.separator)) {
            path = output + subPath;
          } else {
            path = output + File.separator + subPath;
          }

          File f = new File(path);
          if (f.isDirectory()) {
            repoNames.add(path);
          }
        }
      } else {
        repoNames.add(output);
      }
    }

    return repoNames;
  }

  /**
   * This method parses a repository url and extracts the repository name
   *
   * @param repositoryUrl the repository url to parsing
   * @return the repository name
   */
  private String getRepositoryName(String repositoryUrl) {
    System.out.println("Extracting repo from url: " + repositoryUrl);

    // Extract repository name from the URL
    int lastSlashIndex = repositoryUrl.lastIndexOf('/');
    int lastDotIndex = repositoryUrl.lastIndexOf('.');
    return repositoryUrl.substring(lastSlashIndex + 1, lastDotIndex);
  }
}

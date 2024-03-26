package edu.university.ecs.lab.intermediate.create;

import edu.university.ecs.lab.common.config.ConfigUtil;
import edu.university.ecs.lab.common.config.InputConfig;
import edu.university.ecs.lab.common.config.InputRepository;
import edu.university.ecs.lab.common.models.*;
import edu.university.ecs.lab.common.models.enums.ClassRole;
import edu.university.ecs.lab.common.utils.MsFileUtils;
import edu.university.ecs.lab.common.writers.MsJsonWriter;
import edu.university.ecs.lab.intermediate.create.services.GitCloneService;
import edu.university.ecs.lab.intermediate.create.services.RestModelService;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IntermediateExtraction is the main entry point for the intermediate extraction process.
 *
 * <p>The IR extraction process is responsible for cloning remote services, scanning through each
 * local repo and extracting rest endpoints/calls, and writing each service and endpoints to
 * intermediate representation.
 *
 * <p>
 */
public class RestExtraction {
  /** Exit code: error writing IR to json */
  private static final int BAD_IR_WRITE = 3;

  /** system property for user directory */
 // private static final String SYS_USER_DIR = "user.dir";

  /**
   * Main method entry point to intermediate extraction
   *
   * @param args (optional) /path/to/config/file, defaults to config.json in the project directory.
   */
  public static void main(String[] args) throws Exception {
    // Get input config
    String jsonFilePath = (args.length == 1) ? args[0] : "config.json";
    InputConfig inputConfig = ConfigUtil.validateConfig(jsonFilePath);

    // Clone remote repositories and scan through each cloned repo to extract endpoints
    Map<String, MsModel> msDataMap = cloneAndScanServices(inputConfig);

    assert msDataMap != null;

    // Scan through each endpoint to update rest call destinations
    extractCallDestinations(msDataMap);

    //  Write each service and endpoints to IR
    try {
      writeToIntermediateRepresentation(inputConfig, msDataMap);
    } catch (IOException e) {
      System.err.println("Error writing to IR json: " + e.getMessage());
      System.exit(BAD_IR_WRITE);
    }
  }

  /**
   * Write each service and endpoints to intermediate representation
   *
   * @param inputConfig the config file object
   * @param msEndpointsMap a map of service to their information
   */
  private static void writeToIntermediateRepresentation(
      InputConfig inputConfig, Map<String, MsModel> msEndpointsMap) throws IOException {

    String outputPath = inputConfig.getOutputPath();

    File outputDir = new File(outputPath);

    if (!outputDir.exists()) {
      if (outputDir.mkdirs()) {
        System.out.println("Successfully created output directory.");
      } else {
        System.err.println("Failed to create output directory.");
        return;
      }
    }

    Scanner scanner = new Scanner(System.in); // read system name from command line
    System.out.println("Enter system name: ");
    JsonObject jout =
        MsFileUtils.constructJsonMsSystem(scanner.nextLine(), "0.0.1", msEndpointsMap);

    String outputName =
        outputPath
            + File.separator
            + "rest-extraction-output-["
            + (new Date()).getTime()
            + "].json";

    MsJsonWriter.writeJsonToFile(jout, outputName);
    System.out.println("Successfully wrote rest extraction to: \"" + outputName + "\"");
  }

  /**
   * Clone remote repositories and scan through each local repo and extract endpoints/calls
   *
   * @param inputConfig the input config object
   * @return a map of services and their endpoints
   */
  private static Map<String, MsModel> cloneAndScanServices(InputConfig inputConfig)
      throws Exception {
    Map<String, MsModel> msModelMap = new HashMap<>();

    // Clone remote repositories
    String clonePath = inputConfig.getClonePath();

    File cloneDir = new File(clonePath);
    if (!cloneDir.exists()) {
      if (cloneDir.mkdirs()) {
        System.out.println("Successfully created \"" + clonePath + "\" directory.");
      } else {
        System.err.println("Could not create clone directory");
        return null;
      }
    }

    for (InputRepository inputRepository : inputConfig.getRepositories()) {
      MsModel model;
      GitCloneService gitCloneService = new GitCloneService(clonePath);
      List<String> msPathRoots = gitCloneService.cloneRemote(inputRepository);

      // Scan through each local repo and extract endpoints/calls
      for (String msPath : msPathRoots) {
        String path = msPath;

        if (msPath.contains(clonePath) && msPath.length() > clonePath.length() + 1) {
          path = msPath.substring(clonePath.length() + 1);
        }

        model =
            RestModelService.recursivelyScanFiles(clonePath, msPath.substring(clonePath.length()));
        assert model != null;

        model.setCommit(inputRepository.getBaseCommit());
        model.setId(msPath.substring(msPath.lastIndexOf(File.separator) + 1));
        model.setMsPath(msPath);

        msModelMap.put(path, model);
      }
    }

    return msModelMap;
  }

  private static void extractCallDestinations(Map<String, MsModel> msModelMap) {
    msModelMap.forEach((name, model) -> {
      model.getServices().forEach(service -> {
        service.getRestCalls().forEach(call -> {
          String callUrl = call.getApi();
          String httpMethod = call.getHttpMethod();

          JController matchingController = null;

          // iterate until either endpoint is found OR entire URL has been scanned
          while (Objects.isNull(matchingController) && callUrl.contains("/")) {
            final String tmpCallUrl = callUrl;

            for (MsModel ms : msModelMap.values()) {
              matchingController = ms.getControllers().stream().filter(controller ->
                      controller.getEndpoints().stream().anyMatch(endpoint ->
                              (endpoint.getUrl().contains(tmpCallUrl) || endpoint.getHttpMethod().contains(httpMethod))))
                      .findFirst().orElse(null);

              if (Objects.nonNull(matchingController)) {
                break;
              }
            }

            // Endpoint still not found? Try chopping off beginning of url by each '/'
            if (Objects.isNull(matchingController)) {
              callUrl = callUrl.substring(1);

              int slashNdx = callUrl.indexOf("/");
              if (slashNdx > 0) {
                callUrl = callUrl.substring(slashNdx);
              }
            }
          }

          if (Objects.nonNull(matchingController)) {
            call.setDestFile(matchingController.getClassPath());
          }
        });
      });
    });
  }
}

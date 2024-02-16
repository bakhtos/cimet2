package edu.university.ecs.lab.radsource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.university.ecs.lab.radsource.rad.context.RequestContext;
import edu.university.ecs.lab.radsource.rad.context.ResponseContext;
import edu.university.ecs.lab.radsource.rad.service.RestDiscoveryService;

import java.io.FileWriter;
import java.io.IOException;

/**
 * This class is the CLI runner for the RAD CLI.
 *
 * @author Dipta Das
 */
public class RadCLI {

  public static void main(String[] args) throws IOException {
    RestDiscoveryService restDiscoveryService = new RestDiscoveryService();
    RequestContext request = new RequestContext("target", "edu/baylor/ecs", null);

    ResponseContext responseContext = restDiscoveryService.generateResponseContext(request);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    FileWriter fw = new FileWriter("output.json");

    gson.toJson(responseContext, fw);
    fw.close();
  }
}

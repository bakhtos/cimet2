package edu.university.ecs.lab.intermediate.utils;

import edu.university.ecs.lab.common.models.Dependency;
import edu.university.ecs.lab.common.models.Endpoint;
import edu.university.ecs.lab.common.models.MsModel;

import javax.json.*;
import java.util.List;
import java.util.Map;

public class MsFileUtils {
  public static JsonObject constructJsonMsSystem(
      String systemName, String version, Map<String, MsModel> msEndpointsMap) {
    JsonObjectBuilder parentBuilder = Json.createObjectBuilder();

    parentBuilder.add("systemName", systemName);
    parentBuilder.add("version", version);

    JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

    for (Map.Entry<String, MsModel> microservice : msEndpointsMap.entrySet()) {
      JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
      jsonObjectBuilder.add("msName", microservice.getKey());

      JsonArrayBuilder endpointsArrayBuilder = Json.createArrayBuilder();

      List<Endpoint> endpoints = microservice.getValue().getEndpoints();
      for (Endpoint endpoint : endpoints) {
        JsonObjectBuilder endpointBuilder = Json.createObjectBuilder();

        endpointBuilder.add("api", endpoint.getUrl());
        endpointBuilder.add("source-file", endpoint.getSourceFile());
        endpointBuilder.add("type", endpoint.getRestType());

        endpointsArrayBuilder.add(endpointBuilder.build());
      }
      jsonObjectBuilder.add("endpoints", endpointsArrayBuilder.build());

      List<Dependency> dependencies = microservice.getValue().getDependencies();
      for (Dependency dependency : dependencies) {
        JsonObjectBuilder endpointBuilder = Json.createObjectBuilder();

        endpointBuilder.add("api", dependency.getUrl());
        endpointBuilder.add("source-file", dependency.getSourceFile());
        endpointBuilder.add("call-dest", dependency.getDestFile());
        endpointBuilder.add("call-method", dependency.getCallType() + "()");

        endpointsArrayBuilder.add(endpointBuilder.build());
      }
      jsonObjectBuilder.add("dependencies", endpointsArrayBuilder.build());

      jsonArrayBuilder.add(jsonObjectBuilder.build());
    }

    parentBuilder.add("services", jsonArrayBuilder.build());
    return parentBuilder.build();
  }
}
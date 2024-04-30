package edu.university.ecs.lab.common.models;

import com.google.gson.annotations.SerializedName;
import edu.university.ecs.lab.common.models.enums.ClassRole;
import lombok.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.List;

import static edu.university.ecs.lab.common.utils.ObjectToJsonUtils.*;

/**
 * Represents a class in Java. It holds all information regarding that class including all method
 * declarations, method calls, fields, etc.
 */
@Data
@ToString
@Builder
public class JClass implements JsonSerializable {
  protected String className;
  /** Path like repoName/.../serviceName/.../file.java */
  protected String classPath;
  protected String packageName;
  protected ClassRole classRole;
  protected List<Method> methods;

  @SerializedName("variables")
  protected List<Field> fields;

  protected List<MethodCall> methodCalls;

  /** The associated microservice object for this class */
  protected String msId;

  /** Uniquely identify a class as an object of a given service */
  @SerializedName("id")
  public String getId() {
    return classRole.name() + ":" + msId + "#" + className;
  }

  public void setClassPath(String classPath) {
    this.classPath = classPath.replaceAll("\\\\", "/");
  }

  public JClass(String className,
                String classPath,
                String packageName,
                ClassRole classRole,
                List<Method> methods,
                List<Field> fields,
                List<MethodCall> methodCalls,
                String msId) {
    this.className = className;
    setClassPath(classPath);
    this.packageName = packageName;
    this.classRole = classRole;
    this.methods = methods;
    this.fields = fields;
    this.methodCalls = methodCalls;
    this.msId = msId;
  }

  /**
   * Convert a single JClass to a JsonObject
   *
   * @return Converted JsonObject of JClass object
   */
  @Override
  public JsonObject toJsonObject() {
    return createBuilder().build();
  }

  protected JsonObjectBuilder createBuilder() {
    JsonObjectBuilder jClassBuilder = Json.createObjectBuilder();

    jClassBuilder.add("className", this.className);
    jClassBuilder.add("classPath", this.classPath);
    jClassBuilder.add("packageName", this.packageName);
    jClassBuilder.add("classRole", this.classRole.name());
    jClassBuilder.add("msId", msId);
    jClassBuilder.add("methods", listToJsonArray(methods));
    jClassBuilder.add("variables", listToJsonArray(fields));
    jClassBuilder.add("methodCalls", listToJsonArray(methodCalls));

    return jClassBuilder;
  }

  /**
   * Check if the given class is the same as this class. This is true if they have the same classPath.
   *
   * @param other The class to compare with
   * @return True if the classes are the same, false otherwise
   */
  public boolean matchClassPath(JClass other) {
    return this.getClassPath().equals(other.getClassPath());
  }
}

package edu.university.ecs.lab.common.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class JController extends JClass {
  public JController(@NonNull JClass jClass) {
    classPath = jClass.getClassPath();
    packageName = jClass.getPackageName();
    className = jClass.getClassName();
    methods = jClass.getMethods();
    fields = jClass.getFields();
    methodCalls = jClass.getMethodCalls();
  }

  @SerializedName("restEndpoints")
  private List<Endpoint> endpoints;
}

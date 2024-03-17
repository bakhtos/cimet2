package edu.university.ecs.lab.common.models.rest;

import edu.university.ecs.lab.common.models.JavaClass;

public class RestRepository extends JavaClass {
  public RestRepository(JavaClass javaClass) {
    this.setClassName(javaClass.getClassName());
    this.setClassPath(javaClass.getClassPath());
    this.setVariables(javaClass.getVariables());
    this.setMethods(javaClass.getMethods());
  }
}

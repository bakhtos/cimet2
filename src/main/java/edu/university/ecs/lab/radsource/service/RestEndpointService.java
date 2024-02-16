package edu.university.ecs.lab.radsource.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import edu.university.ecs.lab.radsource.model.RestEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RestEndpointService {
    public List<RestEndpoint> findRestEndpoints(File sourceFile) throws IOException {
        List<RestEndpoint> restEndpoints = new ArrayList<>();

        CompilationUnit cu = StaticJavaParser.parse(sourceFile);

        // don't analyse further if no RestController import exists
        if (!hasRestControllerImport(cu)) {
            log.debug("no RestController found");
            return restEndpoints;
        }

        String packageName = Helper.findPackage(cu);
        log.debug("package: " + packageName);

        // loop through class declarations
        for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = cid.getNameAsString();
            log.debug("class: " + className);

            AnnotationExpr cae = cid.getAnnotationByName("RequestMapping").orElse(null);
            String classLevelPath = pathFromAnnotation(cae);
            log.debug("class-level-path: " + classLevelPath);

            // loop through methods
            for (MethodDeclaration md : cid.findAll(MethodDeclaration.class)) {
                String methodName = md.getNameAsString();
                log.debug("method: " + methodName);

                // loop through annotations
                for (AnnotationExpr ae : md.getAnnotations()) {
                    RestEndpoint restEndpoint = new RestEndpoint();

                    switch (ae.getNameAsString()) {
                        case "GetMapping":
                            restEndpoint.setHttpMethod("GET");
                            break;
                        case "PostMapping":
                            restEndpoint.setHttpMethod("POST");
                            break;
                        case "DeleteMapping":
                            restEndpoint.setHttpMethod("DELETE");
                            break;
                        case "PutMapping":
                            restEndpoint.setHttpMethod("PUT");
                            break;
                        case "RequestMapping":
                            if (ae.toString().contains("RequestMethod.POST")) {
                                restEndpoint.setHttpMethod("POST");
                            } else if (ae.toString().contains("RequestMethod.DELETE")) {
                                restEndpoint.setHttpMethod("DELETE");
                            } else if (ae.toString().contains("RequestMethod.PUT")) {
                                restEndpoint.setHttpMethod("PUT");
                            } else {
                                restEndpoint.setHttpMethod("GET");
                            }
                            break;
                    }

                    if (restEndpoint.getHttpMethod() == null) {
                        continue;
                    }

                    String path = pathFromAnnotation(ae);
                    log.debug("method-level-path: " + path);

                    resolveReturnTypeForMethodDeclaration(cu, md, restEndpoint);
                    resolveArgumentsForMethodDeclaration(cu, md, restEndpoint);

                    restEndpoint.setSource(sourceFile.getCanonicalPath());
                    restEndpoint.setParentMethod(packageName + "." + className + "." + methodName);
                    restEndpoint.setPath(Helper.mergePaths(classLevelPath, path));

                    log.debug("rest-endpoint: " + restEndpoint);

                    restEndpoints.add(restEndpoint);
                }
            }
        }

        return restEndpoints;
    }

    // populate return type in restEndpoint
    private void resolveReturnTypeForMethodDeclaration(CompilationUnit cu, MethodDeclaration md, RestEndpoint restEndpoint) {
        String returnType = md.getTypeAsString();

        log.debug("return-type: " + md.getTypeAsString());

        if (returnType.startsWith("List<") && returnType.endsWith(">")) {
            returnType = returnType.replace("List<", "").replace(">", "");
            restEndpoint.setCollection(true);
        } else if (returnType.endsWith("[]")) {
            returnType = returnType.replace("[]", "");
            restEndpoint.setCollection(true);
        }

        restEndpoint.setReturnType(Helper.findFQClassName(cu, returnType));
    }

    // populate arguments in restEndpoint
    // TODO: find FQ name?
    private void resolveArgumentsForMethodDeclaration(CompilationUnit cu, MethodDeclaration md, RestEndpoint restEndpoint) {
        String arguments = md.getParameters().toString();
        log.debug("arguments: " + arguments);
        restEndpoint.setArguments(arguments);
    }

    private boolean hasRestControllerImport(CompilationUnit cu) {
        for (ImportDeclaration id : cu.findAll(ImportDeclaration.class)) {
            if (id.getNameAsString().equals("org.springframework.web.bind.annotation.RestController")) {
                return true;
            } else if (id.getNameAsString().equals("org.springframework.web.bind.annotation") && id.isAsterisk()) {
                return true;
            }
        }
        return false;
    }

    private String pathFromAnnotation(AnnotationExpr ae) {
        if (ae == null) {
            return "";
        }

        log.debug("annotation-model: " + ae.getMetaModel());

        if (ae.isSingleMemberAnnotationExpr()) {
            return Helper.removeEnclosedQuotations(ae.asSingleMemberAnnotationExpr().getMemberValue().toString());
        }

        if (ae.isNormalAnnotationExpr() && ae.asNormalAnnotationExpr().getPairs().size() > 0) {
            for (MemberValuePair mvp : ae.asNormalAnnotationExpr().getPairs()) {
                if (mvp.getName().toString().equals("path") || mvp.getName().toString().equals("value")) {
                    return Helper.removeEnclosedQuotations(mvp.getValue().toString());
                }
            }
        }

        return "";
    }
}

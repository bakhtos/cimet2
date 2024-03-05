package edu.university.ecs.lab.semantics.services;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.BinaryExprMetaModel;
import edu.university.ecs.lab.semantics.models.*;
import edu.university.ecs.lab.semantics.models.enums.ClassRole;
import javassist.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParserService {

    /**
     * Parses method information and creates a Method object representing the method
     *
     * @param n the MethodDeclaration that will be parsed
     * @param role the role of the current class file
     */
    public static void parseMethod(MethodDeclaration n, ClassRole role, Id id) {
        Method method = new Method();
        method.setReturnType(n.getTypeAsString());
        method.setMethodName(n.getNameAsString());
        method.setLine(n.getBegin().get().line);
        method.setAnnotations(parseAnnotations(n.getAnnotations()));

        // Method parameters
        NodeList<com.github.javaparser.ast.body.Parameter> parameters = n.getParameters();
        parameters.stream().forEach(parameter -> method.addArgument(new Parameter(parameter.getTypeAsString())));


        Optional<Node> parentNode = n.getParentNode();
        if (parentNode.isPresent()) {
            // Set Class
            ClassOrInterfaceDeclaration cl = (ClassOrInterfaceDeclaration) parentNode.get();
            method.setClassName(cl.getName().getIdentifier());
            // Find Package
            parentNode = parentNode.get().getParentNode();
            if (parentNode.isPresent()) {
                // Set Package

                if (parentNode.get() instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) parentNode.get();
                    Optional<PackageDeclaration> pd = cu.getPackageDeclaration();
                    pd.ifPresent(
                            packageDeclaration -> method.setPackageName(packageDeclaration.getNameAsString()));
                }

                if (parentNode.get() instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) n.getParentNode().get();
                    if (c.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
                        c = (ClassOrInterfaceDeclaration) c.getParentNode().get();
                    }

                    CompilationUnit cu = (CompilationUnit) c.getParentNode().get();
                    Optional<PackageDeclaration> pd = cu.getPackageDeclaration();
                    pd.ifPresent(packageDeclaration -> method.setPackageName(packageDeclaration.getNameAsString()));
                }
            }
        }
//        method.setIds();
        method.setId(id);
        CachingService.getCache().getMethodList().add(method);
    }

    /**
     * Parse a list of annotation expressions into a List of Annotations
     *
     * @param annotationExprs the annotation expressions to parse
     * @return the list of annotations
     */
    public static List<Annotation> parseAnnotations(NodeList<AnnotationExpr> annotationExprs) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationExpr an : annotationExprs) {
            annotations.add(parseAnnotation(an));
        }
        return annotations;
    }

    /**
     * Parse an annotation expression into an annotation
     *
     * @param annotationExpr the annotation expression to parse
     * @return the annotation
     */
    private static Annotation parseAnnotation(AnnotationExpr annotationExpr) {
        Annotation annotation = new Annotation();
        annotation.setAnnotationName(annotationExpr.getNameAsString());
        List<Node> childNodes = annotationExpr.getChildNodes();
        for (Node node : childNodes) {
            if (node instanceof MemberValuePair) {
                MemberValuePair memberValuePair = (MemberValuePair) node;
                annotation.setKey(memberValuePair.getNameAsString());
                annotation.setValue(memberValuePair.getValue().toString());
                annotation.setHttpAnnotation(true);
            } else if(node instanceof StringLiteralExpr) {
                StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
                annotation.setValue(stringLiteralExpr.getValue());
            }
        }
        return annotation;
    }

    /**
     * Parse a rest call from a MethodCallExpr
     *
     * @param n The method call to parse
     * @return the rest call
     */
    public static RestCall parseRestCall(MethodCallExpr n) {
        // ms cache add MsRestCall
        RestCall msRestCall = new RestCall();
        msRestCall.setStatementDeclaration(n.toString());
        // here try to print the n
        NodeList<Expression> expressionNodeList = n.getArguments();
        for(Expression e : expressionNodeList) {
            if (e instanceof StringLiteralExpr) {
                StringLiteralExpr se = (StringLiteralExpr) e;
                msRestCall.setApiEndpoint(se.toString());
            }
            if (e instanceof BinaryExpr) {
                BinaryExpr be = (BinaryExpr) e;
                msRestCall.setApiEndpoint(be.toString());
            }
            if (e instanceof FieldAccessExpr) {
                FieldAccessExpr f = (FieldAccessExpr) e;
                // GET, POST, etc.
                msRestCall.setHttpMethod(f.getName().toString());
            }
            if (e instanceof NameExpr) {
                NameExpr ne = (NameExpr) e;
                msRestCall.setReturnType(ne.toString());
            }
            if (e instanceof ObjectCreationExpr) {
                ObjectCreationExpr oce = (ObjectCreationExpr) e;
                ClassOrInterfaceType paramType = oce.getType();
                Optional<NodeList<Type>> optParamTypes = paramType.getTypeArguments();
                if(optParamTypes.isPresent()) {
                    for (Type p : optParamTypes.get()) {
                        if (p instanceof ClassOrInterfaceType) {
                            ClassOrInterfaceType tp = (ClassOrInterfaceType) p;
                            if (tp.getTypeArguments().isPresent()) {
                                if(tp.getTypeArguments().isPresent()){
                                    for (Type ta : tp.getTypeArguments().get()) {
                                        msRestCall.setReturnType(ta.toString());
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        return msRestCall;
    }

}

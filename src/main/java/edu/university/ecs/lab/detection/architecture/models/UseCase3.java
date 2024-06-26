package edu.university.ecs.lab.detection.architecture.models;

import edu.university.ecs.lab.common.models.Endpoint;
import edu.university.ecs.lab.common.models.JClass;
import edu.university.ecs.lab.common.models.Microservice;
import edu.university.ecs.lab.common.models.MicroserviceSystem;
import edu.university.ecs.lab.common.models.RestCall;
import edu.university.ecs.lab.detection.architecture.models.enums.Scope;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

/**
 *
 */
@Data
public class UseCase3 extends UseCase {
    protected static final String NAME = "Floating call due to invalid call creation";
    protected static final Scope SCOPE = Scope.REST_CALL;
    protected static final String DESC = "A rest call is added that references a nonexistent endpoint";
    protected JsonObject metaData;

    private UseCase3() {}

    @Override
    public List<? extends UseCase> checkUseCase() {
        ArrayList<UseCase3> useCases = new ArrayList<>();

        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public Scope getScope() {
        return SCOPE;
    }

    @Override
    public double getWeight() {
        return 0;
    }

    @Override
    public JsonObject getMetaData() {
        return metaData;
    }

    public static UseCase3 scan(RestCall restCall, MicroserviceSystem microserviceSystem){
        for (Microservice microservice : microserviceSystem.getMicroservices()){
            for(JClass controller : microservice.getControllers()){
                for (Endpoint endpoint : controller.getEndpoints()){
                    if (RestCall.matchEndpoint(restCall, endpoint)){
                        return null;
                    }
                }
            }
        }

        UseCase3 useCase3 = new UseCase3();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("Rest Call", restCall.toJsonObject());
        useCase3.setMetaData(jsonObject);    
        return useCase3;
    }
}

package edu.university.ecs.lab.radsource.context;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RadSourceRequestContext {
    private List<String> pathToMsRoots;
}
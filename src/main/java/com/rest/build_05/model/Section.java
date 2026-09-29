package com.rest.build_05.model;

import lombok.Data;

@Data 
public class Section {
    private Long id;
    private Long courseId;
    private String termId;    
    private int capacity;
    private int enrolled;
}
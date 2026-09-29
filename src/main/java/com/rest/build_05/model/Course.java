package com.rest.build_05.model;

import lombok.Data;

@Data 
public class Course {
    private Long id;
    private String code;      
    private String title;
    private String subject;   
}
package com.focusfortress.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryStructure {
    private String name;
    private String icon;
    private List<SubcategoryStructure> subcategories;
}

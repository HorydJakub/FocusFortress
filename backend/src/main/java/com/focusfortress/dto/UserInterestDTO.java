package com.focusfortress.dto;

import com.focusfortress.model.Category;
import com.focusfortress.model.Subcategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestDTO {
    private Long id;
    private String subcategoryName;
    private String subcategoryIcon;
    private String categoryName;
    private String categoryIcon;
    private LocalDateTime selectedAt;

    public static UserInterestDTO fromEntity(com.focusfortress.model.UserInterest entity) {
        Subcategory subcategory = entity.getSubcategory();
        Category category = subcategory.getCategory();

        return new UserInterestDTO(
                entity.getId(),
                subcategory.getName(),
                subcategory.getIcon(),
                category.getName(),
                category.getIcon(),
                entity.getSelectedAt()
        );
    }
}

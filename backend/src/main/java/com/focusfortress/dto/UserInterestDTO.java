package com.focusfortress.dto;

import com.focusfortress.model.InterestCategory;
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
    private String interestKey;
    private String displayName;
    private String icon;
    private LocalDateTime selectedAt;

    public static UserInterestDTO fromEntity(com.focusfortress.model.UserInterest entity) {
        InterestCategory ic = entity.getInterest();
        return new UserInterestDTO(
                entity.getId(),
                ic.name(),
                ic.getDisplayName(),
                ic.getIcon(),
                entity.getSelectedAt()
        );
    }
}

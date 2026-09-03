package com.mentalhealthforum.mentalhealthforum_backend.validation.group;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AssignableGroupValidator implements ConstraintValidator<ValidAssignableGroup, GroupPath> {

    @Override
    public boolean isValid(GroupPath group, ConstraintValidatorContext context) {
        if(group == null) return true; // @NotNull handles null

        //  Technical Check: Is it a leaf group that grants roles?
        if(!group.isAssignable()){
            buildViolation(context, String.format(
                    "Group '%s' is not assignable. Please select a subgroup like 'MEMBERS_NEW', 'MEMBERS_TRUSTED', etc.", group.getPath()
            ));
            return false;
        }

        return true;
    }

    private void buildViolation(ConstraintValidatorContext context, String message){
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}

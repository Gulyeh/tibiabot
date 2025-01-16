package mongo.models;

import cache.enums.Roles;
import lombok.Getter;

@Getter
public class RolesModel {
    private String dromeRole = "";

    public void setByType(Roles role, String roleId) {
        switch (role) {
            case DROME -> dromeRole = roleId;
        }
    }
}

package apis.tibiaData.model.information;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Status {
    private int http_code;
    private int error;
    @Setter
    private String message;
}

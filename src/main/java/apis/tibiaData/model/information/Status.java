package apis.tibiaData.model.information;

import lombok.Getter;

@Getter
public class Status {
    private int http_code;
    private int error;
    private String message;
}

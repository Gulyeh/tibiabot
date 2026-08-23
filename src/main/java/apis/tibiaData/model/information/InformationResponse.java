package apis.tibiaData.model.information;

import lombok.Getter;

@Getter
public class InformationResponse {
    private final Status status = new Status();
}

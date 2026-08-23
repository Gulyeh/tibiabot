package apis.tibiaTrade.model;


import lombok.Getter;

@Getter
public class BaseResponseModel<T> {
    private BaseResultModel<T> result;
}

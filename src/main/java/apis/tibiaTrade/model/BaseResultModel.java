package apis.tibiaTrade.model;

import lombok.Getter;

@Getter
public class BaseResultModel<T> {
    private T data;
}

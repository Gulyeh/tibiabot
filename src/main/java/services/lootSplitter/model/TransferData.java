package services.lootSplitter.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class TransferData {
    private String transferTo;
    private String transferAmount;
}

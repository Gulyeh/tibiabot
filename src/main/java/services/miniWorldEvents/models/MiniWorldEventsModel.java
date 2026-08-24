package services.miniWorldEvents.models;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.List;

@Getter
public class MiniWorldEventsModel {
    @Nullable
    private List<MiniWorldEvent> active_mini_world_changes;
}

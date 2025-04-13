package mongo.models;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class DeathFilter {
    private final Set<String> names = new HashSet<>();
    private final Set<String> guilds = new HashSet<>();
}

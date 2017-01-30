package xyz.nickr.telegram.omnibot.tv;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.jomdb.model.SeasonEpisodeResult;
import xyz.nickr.jomdb.model.SeasonResult;

/**
 * @author Nick Robson
 */
@Getter
@AllArgsConstructor
public class Season {

    private String id;
    private Episode[] episodes;

    public Season(SeasonResult seasonResult) {
        this.id = seasonResult.getSeason();

        SeasonEpisodeResult[] episodes = seasonResult.getEpisodes();
        this.episodes = new Episode[episodes.length];
        for (int i = 0, j = episodes.length; i < j; i++)
            this.episodes[i] = new Episode(episodes[i]);
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("schema", 1)
                .append("episodes", Arrays.stream(this.episodes)
                        .map(Episode::toDocument)
                        .collect(Collectors.toList()));
    }

}

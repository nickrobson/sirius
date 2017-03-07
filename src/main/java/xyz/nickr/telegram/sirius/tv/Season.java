package xyz.nickr.telegram.sirius.tv;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.filmfo.model.season.SeasonEpisode;

/**
 * @author Nick Robson
 */
@Getter
@AllArgsConstructor
public class Season {

    private int id;
    private Episode[] episodes;

    public Season(xyz.nickr.filmfo.model.season.Season seasonResult) {
        this.id = seasonResult.getSeason();

        SeasonEpisode[] episodes = seasonResult.getEpisodes();
        this.episodes = new Episode[episodes.length];
        for (int i = 0, j = episodes.length; i < j; i++)
            this.episodes[i] = new Episode(episodes[i]);
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("episodes", Arrays.stream(this.episodes)
                        .map(Episode::toDocument)
                        .collect(Collectors.toList()));
    }

}

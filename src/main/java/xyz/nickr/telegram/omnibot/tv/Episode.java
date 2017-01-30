package xyz.nickr.telegram.omnibot.tv;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.jomdb.model.SeasonEpisodeResult;

/**
 * @author Nick Robson
 */
@Getter
@AllArgsConstructor
public class Episode {

    private String id;
    private String name;

    public Episode(SeasonEpisodeResult seasonEpisodeResult) {
        this.id = seasonEpisodeResult.getEpisode();
        this.name = seasonEpisodeResult.getTitle();
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("name", name)
                .append("schema", 1);
    }

}

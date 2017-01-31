package xyz.nickr.telegram.sirius.tv;

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
    private String release;
    private String rating;

    public Episode(SeasonEpisodeResult seasonEpisodeResult) {
        this.id = seasonEpisodeResult.getEpisode();
        this.name = seasonEpisodeResult.getTitle();
        this.release = seasonEpisodeResult.getReleased();
        this.rating = seasonEpisodeResult.getImdbRating();
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("name", name)
                .append("rating", rating)
                .append("release", release);
    }

}

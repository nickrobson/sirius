package xyz.nickr.telegram.sirius.tv;

import java.time.LocalDateTime;
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
    private String imdbId;
    private LocalDateTime releaseDate;

    public Episode(String id, String name, String release, String rating, String imdbId) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.imdbId = imdbId;
        this.release = release;
        this.releaseDate = SeasonEpisodeResult.parseReleaseDate(release);
    }

    public Episode(SeasonEpisodeResult res) {
        this(res.getEpisode(), res.getTitle(), res.getRelease(), res.getImdbRating(), res.getImdbId(), res.getReleaseDate());
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("name", name)
                .append("rating", rating)
                .append("release", release)
                .append("imdb", imdbId);
    }

}

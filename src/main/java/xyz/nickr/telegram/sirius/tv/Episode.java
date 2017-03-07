package xyz.nickr.telegram.sirius.tv;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.filmfo.Filmfo;
import xyz.nickr.filmfo.model.season.SeasonEpisode;

/**
 * @author Nick Robson
 */
@Getter
@AllArgsConstructor
public class Episode {

    private int id;
    private String name;
    private String release;
    private String imdbId;
    private LocalDateTime releaseDate;

    public Episode(int id, String name, String release, String imdbId) {
        this.id = id;
        this.name = name;
        this.imdbId = imdbId;
        this.release = release;
        this.releaseDate = Filmfo.parseDate(release);
    }

    public Episode(SeasonEpisode res) {
        this(res.getEpisode(), res.getName(), res.getDate(), res.getId(), Filmfo.parseDate(res.getDate()));
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("name", name)
                .append("release", release)
                .append("imdb", imdbId);
    }

}

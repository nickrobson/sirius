package xyz.nickr.telegram.sirius.tv;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import lombok.AccessLevel;
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
    private Calendar releaseDate;

    public Episode(String id, String name, String release, String rating, String imdbId) {
        this.id = id;
        this.name = name;
        this.release = release;
        this.rating = rating;
        this.imdbId = imdbId;
        this.releaseDate = null;

        if (release != null && !release.isEmpty() && !"N/A".equals(release)) {
            try {
                int[] parts = Arrays.stream(release.split("-"))
                        .mapToInt(Integer::valueOf)
                        .toArray();
                this.releaseDate = new GregorianCalendar(parts[0] - 1900, parts[1], parts[2]);
            } catch (Exception ex) {
                System.err.println("failed to parse release date: " + release);
                ex.printStackTrace();
            }
        }
    }

    public Episode(SeasonEpisodeResult res) {
        this(res.getEpisode(), res.getTitle(), res.getReleased(), res.getImdbRating(), res.getImdbId(), res.getReleaseDate());
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

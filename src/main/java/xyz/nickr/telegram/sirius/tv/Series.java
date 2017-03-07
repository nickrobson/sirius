package xyz.nickr.telegram.sirius.tv;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.filmfo.Filmfo;
import xyz.nickr.filmfo.FilmfoException;
import xyz.nickr.filmfo.model.title.SeriesTitle;
import xyz.nickr.filmfo.model.title.TitleActor;
import xyz.nickr.filmfo.model.title.TitlePerson;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
@Getter
public class Series {

    private final String imdbId;
    private String name;
    private Season[] seasons;

    private String type;
    private String year;
    private String[] genres;
    private String[] actors;
    private String[] creators;
    private String[] directors;
    private String awards;
    private double rating;
    private long ratingCount;
    private String[] countries;
    private String[] languages;
    private String plot;
    private int runtime;

    private final boolean storeInDatabase;

    private Map.Entry<Season, Episode> lastAiredEpisode, nextAiredEpisode;
    private boolean loadedAiredEpisodes;

    public Series(String imdbId, String name, Season[] seasons, String type, String year, String[] genres, String[] actors, String[] creators, String[] directors, String awards, double rating, long ratingCount, String[] countries, String[] languages, String plot, int runtime) {
        System.err.println("Invoked new Series(" + imdbId + ", " + name + ", ...)");
        this.imdbId = imdbId;
        this.name = name;
        this.seasons = seasons;
        this.type = type;
        this.year = year;
        this.genres = genres;
        this.actors = actors;
        this.creators = creators;
        this.directors = directors;
        this.awards = awards;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.countries = countries;
        this.languages = languages;
        this.plot = plot;
        this.runtime = runtime;

        this.storeInDatabase = true;

        Sirius.getExecutor().submit(this::update);
    }

    public Series(String imdbId) {
        this(imdbId, true);
    }

    public Series(String imdbId, boolean storeInDatabase) {
        System.err.println("Invoked new Series(" + imdbId + ")");
        this.imdbId = imdbId;
        this.storeInDatabase = storeInDatabase;
        update();
    }

    public Map.Entry<Season, Episode> getLastAiredEpisode() {
        if (loadedAiredEpisodes)
            return this.lastAiredEpisode;

        LocalDateTime now = LocalDateTime.now();
        Function<Season, Optional<Episode>> latestEpisodeFunc =
                s -> IntStream.range(0, s.getEpisodes().length)
                        .mapToObj(i -> s.getEpisodes()[s.getEpisodes().length - i - 1])
                        .map(e -> new AbstractMap.SimpleEntry<>(e, e.getReleaseDate()))
                        .filter(e -> (e != null) && (e.getValue() != null) && e.getValue().isBefore(now))
                        .map(Map.Entry::getKey)
                        .findFirst();

        this.lastAiredEpisode = IntStream.range(0, seasons.length)
                .mapToObj(i -> seasons[seasons.length - i - 1])
                .filter(Objects::nonNull)
                .filter(s -> s.getEpisodes().length > 0)
                .map(s -> latestEpisodeFunc.apply(s)
                        .map(e -> new AbstractMap.SimpleEntry<>(s, e))
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        loadedAiredEpisodes = true;

        if (this.lastAiredEpisode == null)
            return null;

        int epSlot = Arrays.asList(lastAiredEpisode.getKey().getEpisodes()).indexOf(lastAiredEpisode.getValue());

        if (epSlot != lastAiredEpisode.getKey().getEpisodes().length - 1) {
            this.nextAiredEpisode = new AbstractMap.SimpleEntry<>(lastAiredEpisode.getKey(), lastAiredEpisode.getKey().getEpisodes()[epSlot + 1]);
        } else {
            int seasonSlot = Arrays.asList(seasons).indexOf(lastAiredEpisode.getKey());
            while (++seasonSlot < seasons.length && seasons[seasonSlot] == null);
            Season season = seasons[seasonSlot];
            if (season != null && season.getEpisodes() != null && season.getEpisodes().length > 0) {
                this.nextAiredEpisode = new AbstractMap.SimpleEntry<>(season, season.getEpisodes()[0]);
            }
        }

        return this.lastAiredEpisode;
    }

    public Map.Entry<Season, Episode> getNextAiredEpisode() {
        if (!loadedAiredEpisodes)
            getLastAiredEpisode();
        return this.nextAiredEpisode;
    }

    public Document toDocument() {
        return new Document("schema", 8)
                .append("id", imdbId)
                .append("name", name)
                .append("type", type)
                .append("year", year)
                .append("genres", Arrays.asList(genres))
                .append("actors", Arrays.asList(actors))
                .append("creators", Arrays.asList(creators))
                .append("directors", Arrays.asList(directors))
                .append("awards", awards)
                .append("rating", rating)
                .append("ratingCount", ratingCount)
                .append("countries", Arrays.asList(countries))
                .append("languages", Arrays.asList(languages))
                .append("plot", plot)
                .append("runtime", runtime)
                .append("seasons", Arrays.stream(this.seasons)
                        .map(Season::toDocument)
                        .collect(Collectors.toList()));
    }

    private <X> String[] map(X[] xs, Function<X, String> func) {
        return Arrays.stream(xs).map(func).collect(Collectors.toList()).toArray(new String[0]);
    }

    public void update() {
        try {
            if (!Filmfo.IMDB_ID_PATTERN.matcher(imdbId).matches())
                throw new IllegalArgumentException("not a valid IMDB id: " + imdbId);

            SeriesTitle title = Sirius.getFilmfo().getSeriesTitle(imdbId);

            if (title == null)
                return;

            this.name = title.getName();
            this.type = title.getType();
            this.year = title.getYear();
            this.genres = title.getGenres();
            this.actors = map(title.getActors(), TitleActor::getName);
            this.creators = map(title.getCreators(), TitlePerson::getName);
            this.directors = map(title.getDirectors(), TitlePerson::getName);
            this.awards = title.getAwards();
            this.rating = title.getRating();
            this.ratingCount = title.getRatingCount();
            this.countries = title.getCountries();
            this.languages = title.getLanguages();
            this.plot = title.getPlot();
            this.runtime = title.getRuntime();

            try {
                int totalSeasons = title.getTotalSeasons();
                this.seasons = new Season[totalSeasons];
                for (int i = 1; i <= totalSeasons; i++) {
                    this.seasons[i - 1] = new Season(Sirius.getFilmfo().getSeason(imdbId, i));
                }
            } catch (FilmfoException ex) {
                ex.printStackTrace();
            }

            if (!this.storeInDatabase)
                return;

            Sirius.getExecutor().submit(() -> {
                MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

                Document document = this.toDocument();
                Document existing = collection.find(Filters.eq("id", imdbId)).projection(Projections.include("schema", "links")).first();

                if (existing != null) {
                    document.append("links", existing.get("links")); // we want to keep the links array
                }

                if (existing == null) {
                    System.out.println("Inserting database series model for id: " + imdbId);
                    collection.insertOne(document);
                } else {
                    int oldSchema = existing.getInteger("schema", -2);
                    int newSchema = document.getInteger("schema", -1);
                    if (oldSchema < newSchema) {
                        System.out.format("Updating database series model for id: %s\nto: %s\n", imdbId, document);
                        collection.replaceOne(Filters.eq("id", imdbId), document);
                    } else if (oldSchema > newSchema) {
                        System.err.format(
                                "[ERROR] Found newer schema in database than in code. Invalid database model?\n" +
                                        "database: %s, code: %s\n",
                                oldSchema,
                                newSchema
                        );
                    }
                }
            });
        } catch (FilmfoException ex) {
            ex.printStackTrace();
        }
    }

}

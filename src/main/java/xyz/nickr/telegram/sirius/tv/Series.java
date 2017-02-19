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
import xyz.nickr.jomdb.JOMDBUnavailableException;
import xyz.nickr.jomdb.JavaOMDB;
import xyz.nickr.jomdb.model.SeasonResult;
import xyz.nickr.jomdb.model.TitleResult;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
@Getter
public class Series {

    private final String imdbId;
    private String name;
    private Season[] seasons;

    private String genre;
    private String actors;
    private String writer;
    private String director;
    private String awards;
    private String country;
    private String type;
    private String rating;
    private String votes;
    private String language;
    private String metascore;
    private String plot;
    private String poster;
    private String runtime;
    private String year;

    private final boolean storeInDatabase;

    private Map.Entry<Season, Episode> lastAiredEpisode, nextAiredEpisode;
    private boolean loadedAiredEpisodes;

    public Series(String imdbId, String name, Season[] seasons, String genre, String actors, String writer, String director, String awards, String country, String type, String rating, String votes, String language, String metascore, String plot, String poster, String runtime, String year) {
        System.out.println("Invoked new Series(" + imdbId + ", " + name + ", [..seasons..])");
        this.imdbId = imdbId;
        this.name = name;
        this.seasons = seasons;
        this.genre = genre;
        this.actors = actors;
        this.writer = writer;
        this.director = director;
        this.awards = awards;
        this.country = country;
        this.type = type;
        this.rating = rating;
        this.votes = votes;
        this.language = language;
        this.metascore = metascore;
        this.plot = plot;
        this.poster = poster;
        this.runtime = runtime;
        this.year = year;

        this.storeInDatabase = true;

        Sirius.getExecutor().submit(this::update);
    }

    public Series(String imdbId) {
        this(imdbId, true);
    }

    public Series(String imdbId, boolean storeInDatabase) {
        System.out.println("Invoked new Series(" + imdbId + ")");
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
                        .filter(e -> (e != null) && (e.getValue() != null) && e.getValue().isBefore(now) && !Sirius.getBotInstance().getCollator().equals("N/A", e.getKey().getRating()))
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
        return new Document("schema", 6)
                .append("id", imdbId)
                .append("name", name)
                .append("genre", genre)
                .append("actors", actors)
                .append("writer", writer)
                .append("director", director)
                .append("awards", awards)
                .append("country", country)
                .append("type", type)
                .append("rating", rating)
                .append("votes", votes)
                .append("language", language)
                .append("metascore", metascore)
                .append("plot", plot)
                .append("poster", poster)
                .append("runtime", runtime)
                .append("year", year)
                .append("seasons", Arrays.stream(this.seasons)
                        .map(Season::toDocument)
                        .collect(Collectors.toList()));
    }

    public void update() {
        try {
            if (!JavaOMDB.IMDB_ID_PATTERN.matcher(imdbId).matches())
                throw new IllegalArgumentException("not a valid IMDB id: " + imdbId);

            TitleResult titleResult = Sirius.getOmdb().titleById(imdbId, true);

            if (titleResult == null)
                return;

            if (!Sirius.getBotInstance().getCollator().equals("series", titleResult.getType()))
                throw new IllegalArgumentException(imdbId + " is a " + titleResult.getType() + " not a series!");

            if (titleResult.getTitle() != null)
                this.name = titleResult.getTitle();
            if (titleResult.getGenre() != null)
                this.genre = titleResult.getGenre();
            if (titleResult.getActors() != null)
                this.actors = titleResult.getActors();
            if (titleResult.getWriter() != null)
                this.writer = titleResult.getWriter();
            if (titleResult.getDirector() != null)
                this.director = titleResult.getDirector();
            if (titleResult.getAwards() != null)
                this.awards = titleResult.getAwards();
            if (titleResult.getCountry() != null)
                this.country = titleResult.getCountry();
            if (titleResult.getType() != null)
                this.type = titleResult.getType();
            if (titleResult.getImdbRating() != null)
                this.rating = titleResult.getImdbRating();
            if (titleResult.getImdbVotes() != null)
                this.votes = titleResult.getImdbVotes();
            if (titleResult.getLanguage() != null)
                this.language = titleResult.getLanguage();
            if (titleResult.getMetascore() != null)
                this.metascore = titleResult.getMetascore();
            if (titleResult.getPlot() != null)
                this.plot = titleResult.getPlot();
            if (titleResult.getPoster() != null)
                this.poster = titleResult.getPoster();
            if (titleResult.getRuntime() != null)
                this.runtime = titleResult.getRuntime();
            if (titleResult.getYear() != null)
                this.year = titleResult.getYear();

            try {
                Season[] seasons = new Season[titleResult.getTotalSeasons()];
                int i = 0;
                for (SeasonResult seasonResult : titleResult) {
                    seasons[i] = new Season(seasonResult);
                    i++;
                }
                if (i == seasons.length)
                    this.seasons = seasons;
            } catch (RuntimeException ex) {
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
        } catch (JOMDBUnavailableException ex) {
            System.err.println(ex.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

package xyz.nickr.telegram.sirius.tv;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.jomdb.model.SeasonResult;
import xyz.nickr.jomdb.model.TitleResult;
import xyz.nickr.telegram.sirius.Sirius;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
@Getter
public class Series {

    private String id;
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

    public Series(String id, String name, Season[] seasons, String genre, String actors,  String writer,  String director,  String awards,  String country,  String type,  String rating,  String votes,  String language,  String metascore,  String plot,  String poster,  String runtime,  String year) {
        System.out.println("Invoked new Series(" + id + ", " + name + ", [..seasons..])");
        this.id = id;
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

        Sirius.getExecutor().submit(this::update);
    }

    public Series(String id) {
        System.out.println("Invoked new Series(" + id + ")");
        this.id = id;
        update();
    }

    public Document toDocument() {
        return new Document("schema", 4)
                .append("id", id)
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
        TitleResult titleResult = Sirius.getOmdb().titleById(id);

        if (!"series".equals(titleResult.getType()))
            throw new IllegalArgumentException(id + " is a " + titleResult.getType() + " not a series!");

        this.name = titleResult.getTitle();
        this.genre = titleResult.getGenre();
        this.actors = titleResult.getActors();
        this.writer = titleResult.getWriter();
        this.director = titleResult.getDirector();
        this.awards = titleResult.getAwards();
        this.country = titleResult.getCountry();
        this.type = titleResult.getType();
        this.rating = titleResult.getImdbRating();
        this.votes = titleResult.getImdbVotes();
        this.language = titleResult.getLanguage();
        this.metascore = titleResult.getMetascore();
        this.plot = titleResult.getPlot();
        this.poster = titleResult.getPoster();
        this.runtime = titleResult.getRuntime();
        this.year = titleResult.getYear();
        this.seasons = new Season[titleResult.getTotalSeasons()];
        int i = 0;
        for (SeasonResult seasonResult : titleResult) {
            this.seasons[i++] = new Season(seasonResult);
        }

        Sirius.getExecutor().submit(() -> {
            MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

            Document document = this.toDocument();
            Document existing = collection.find(eq("id", id)).projection(Projections.include("schema", "links")).first();

            if (existing != null) {
                document.append("links", existing.get("links")); // we want to keep the links array
            }

            if (existing == null) {
                System.out.println("Inserting database series model for id: " + id);
                collection.insertOne(document);
            } else {
                int oldSchema = existing.getInteger("schema", 0);
                int newSchema = document.getInteger("schema", 1);
                if (oldSchema < newSchema) {
                    System.out.println(
                            "Updating database series model for id: " + id + "\n" +
                                    "from: " + existing + "\n" +
                                    "to: " + document
                    );
                    collection.replaceOne(eq("id", id), document);
                } else if (oldSchema > newSchema) {
                    System.err.format(
                            "[ERROR] Found newer schema in database than in code. Invalid database model?\n" +
                                    "database: %s, code: %s\n", oldSchema, newSchema
                    );
                }
            }
        });
    }

}

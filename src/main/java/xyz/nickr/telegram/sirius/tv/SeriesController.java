package xyz.nickr.telegram.sirius.tv;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
public class SeriesController {

    private final Map<String, Series> seriesMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> seriesNamesMap = Collections.synchronizedMap(new HashMap<>());

    public SeriesController() {
        Sirius.getExecutor().submit(() -> {
            MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();

                    String id = doc.getString("id");
                    String name = doc.getString("name");
                    String genre = doc.getString("genre");
                    String actors = doc.getString("actors");
                    String writer = doc.getString("writer");
                    String director = doc.getString("director");
                    String awards = doc.getString("awards");
                    String country = doc.getString("country");
                    String type = doc.getString("type");
                    String rating = doc.getString("rating");
                    String votes = doc.getString("votes");
                    String language = doc.getString("language");
                    String metascore = doc.getString("metascore");
                    String plot = doc.getString("plot");
                    String poster = doc.getString("poster");
                    String runtime = doc.getString("runtime");
                    String year = doc.getString("year");
                    List<Document> seasonsList = (List<Document>) doc.get("seasons");

                    seriesNamesMap.put(id, name);

                    Season[] seasons;
                    if (seasonsList != null) {
                        seasons = new Season[seasonsList.size()];
                        for (int i = 0, j = seasons.length; i < j; i++) {
                            Document seasonDoc = seasonsList.get(i);
                            List<Document> episodesList = (List<Document>) seasonDoc.get("episodes");
                            Episode[] episodes = new Episode[episodesList.size()];

                            for (int k = 0, l = episodes.length; k < l; k++) {
                                Document episodeDoc = episodesList.get(k);

                                String episodeId = episodeDoc.getString("id");
                                String episodeName = episodeDoc.getString("name");
                                String episodeRelease = episodeDoc.getString("release");
                                String episodeRating = episodeDoc.getString("rating");
                                String episodeImdbId = episodeDoc.getString("imdb");

                                episodes[k] = new Episode(episodeId, episodeName, episodeRelease, episodeRating, episodeImdbId);
                            }

                            seasons[i] = new Season(seasonDoc.getString("id"), episodes);
                        }
                    } else {
                        seasons = new Season[0];
                    }
                    seriesMap.put(id, new Series(id, name, seasons, genre, actors, writer, director, awards, country, type, rating, votes, language, metascore, plot, poster, runtime, year));
                }
            }
        });
    }

    public Set<Series> getSeries() {
        return new HashSet<>(this.seriesMap.values());
    }

    public Series getSeries(String id, boolean create) {
        return create
                ? seriesMap.computeIfAbsent(id, Series::new)
                : seriesMap.get(id);
    }

    public Series getSeriesByLink(String link) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

        Document doc = collection.find(Filters.eq("links", link.toLowerCase(Sirius.getBotInstance().getLocale()))).projection(Projections.include("id")).first();

        return (doc != null) ? getSeries(doc.getString("id"), true) : null;
    }

    public String getSeriesName(String id) {
        String name = seriesNamesMap.get(id);
        if (name != null)
            return name;

        Series series = seriesMap.get(id);
        if (series != null)
            return series.getName();

        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

        Document document = collection.find(Filters.eq("id", id)).projection(Projections.include("name")).first();
        if (document == null) {
            return getSeries(id, true).getName();
        }
        Sirius.getExecutor().submit(() -> getSeries(id, true));
        return document.getString("name");
    }

    public Map<String, List<String>> getSeriesLinks() {
        Map<String, List<String>> map = new HashMap<>();

        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

        try (MongoCursor<Document> cursor = collection.find().projection(Projections.include("name", "links")).iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();

                String name = document.getString("name");
                List<String> links = (List<String>) document.get("links");

                map.put(name, links);
            }
        }
        return map;
    }

    public boolean addShow(Series series, String... links) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

        Document document = series.toDocument()
                .append("links", Arrays.stream(links).map(String::toLowerCase).collect(Collectors.toList()));

        try {
            collection.insertOne(document);
            return true;
        } catch (MongoWriteException ex) {
            if (ex.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
                return false;
            throw ex;
        }
    }

    public boolean removeShow(String id) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("shows");

        DeleteResult res = collection.deleteOne(Filters.eq("id", id));
        return res.getDeletedCount() > 0;
    }

}

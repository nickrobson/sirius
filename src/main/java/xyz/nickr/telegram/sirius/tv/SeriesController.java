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
                    try {
                        String name = doc.getString("name");
                        String type = doc.getString("type");
                        String year = doc.getString("year");
                        String[] genres = ((List<String>) doc.get("genres")).toArray(new String[0]);
                        String[] actors = ((List<String>) doc.get("actors")).toArray(new String[0]);
                        String[] creators = ((List<String>) doc.get("creators")).toArray(new String[0]);
                        String[] directors = ((List<String>) doc.get("directors")).toArray(new String[0]);
                        String awards = doc.getString("awards");
                        double rating = doc.getDouble("rating");
                        long ratingCount = doc.getLong("ratingCount");
                        String[] countries = ((List<String>) doc.get("countries")).toArray(new String[0]);
                        String[] languages = ((List<String>) doc.get("languages")).toArray(new String[0]);
                        String plot = doc.getString("plot");
                        int runtime = doc.getInteger("runtime");
                        List<Document> seasonsList = (List<Document>) doc.get("seasons");

                        seriesNamesMap.put(id, name);

                        Season[] seasons = new Season[seasonsList != null ? seasonsList.size() : 0];
                        if (seasonsList != null) {
                            for (int i = 0, j = seasons.length; i < j; i++) {
                                Document seasonDoc = seasonsList.get(i);
                                List<Document> episodesList = (List<Document>) seasonDoc.get("episodes");
                                Episode[] episodes = new Episode[episodesList.size()];

                                for (int k = 0, l = episodes.length; k < l; k++) {
                                    Document episodeDoc = episodesList.get(k);

                                    int episodeId = episodeDoc.getInteger("id");
                                    String episodeName = episodeDoc.getString("name");
                                    String episodeRelease = episodeDoc.getString("release");
                                    String episodeImdbId = episodeDoc.getString("imdb");

                                    episodes[k] = new Episode(episodeId, episodeName, episodeRelease, episodeImdbId);
                                }

                                seasons[i] = new Season(seasonDoc.getInteger("id"), episodes);
                            }
                        }
                        seriesMap.put(id, new Series(id, name, seasons, type, year, genres, actors, creators, directors, awards, rating, ratingCount, countries, languages, plot, runtime));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.err.println("Failed to load " + id + " from database: " + ex + "\nLoading from Filmfo...");
                        seriesMap.put(id, new Series(id));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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

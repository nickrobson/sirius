package xyz.nickr.telegram.omnibot.tv;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import xyz.nickr.telegram.omnibot.OmniBot;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
public class SeriesController {

    private final Map<String, Series> seriesMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> seriesNamesMap = Collections.synchronizedMap(new HashMap<>());

    public SeriesController() {
        OmniBot.getExecutor().submit(() -> {
            MongoCollection<Document> collection = OmniBot.getMongoController().getCollection("shows");

            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();

                    String id = doc.getString("id");
                    String name = doc.getString("name");
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
                                episodes[k] = new Episode(episodeDoc.getString("id"), episodeDoc.getString("name"));
                            }

                            seasons[i] = new Season(seasonDoc.getString("id"), episodes);
                        }
                    } else {
                        seasons = new Season[0];
                    }
                    seriesMap.put(id, new Series(id, name, seasons));
                }
            }
        });
    }

    public Series getSeries(String id, boolean create) {
        return create
                ? seriesMap.computeIfAbsent(id, Series::new)
                : seriesMap.get(id);
    }

    public Series getSeriesByLink(String link) {
        MongoCollection<Document> collection = OmniBot.getMongoController().getCollection("shows");

        Document doc = collection.find(eq("links", link)).first();

        return doc != null ? getSeries(doc.getString("id"), true) : null;
    }

    public String getSeriesName(String id) {
        String name = seriesNamesMap.get(id);
        if (name != null)
            return name;

        Series series = seriesMap.get(id);
        if (series != null)
            return series.getName();

        MongoCollection<Document> collection = OmniBot.getMongoController().getCollection("shows");

        Document document = collection.find(eq("id", id)).first();
        if (document == null) {
            return getSeries(id, true).getName();
        }
        OmniBot.getExecutor().submit(() -> getSeries(id, true));
        return document.getString("name");
    }
}

package xyz.nickr.telegram.tvchatbot.tv;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.user.User;
import xyz.nickr.telegram.tvchatbot.TvChatBot;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
public class ProgressController {

    public Map<String, String> getProgress(User user) {
        MongoCollection<Document> collection = TvChatBot.getMongoController().getCollection("progress");

        Map<String, String> progress = new HashMap<>();
        try (MongoCursor<Document> cursor = collection.find(eq("user", user.getId())).iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();

                String id = document.getString("id");
                String episode = document.getString("episode");

                progress.put(id, episode);
            }
        }
        return progress;
    }

    public String getProgress(User user, String id) {
        MongoCollection<Document> collection = TvChatBot.getMongoController().getCollection("progress");

        Document document = collection.find(and(eq("id", id), eq("user", user.getId()))).first();

        return document != null ? document.getString("episode") : null;
    }

    public void setProgress(User user, String id, String progress) {
        MongoCollection<Document> collection = TvChatBot.getMongoController().getCollection("progress");

        Document doc = new Document()
                .append("id", id)
                .append("user", user.getId())
                .append("episode", progress);

        collection.updateOne(and(eq("id", id), eq("user", user.getId())), new Document("$set", doc));
    }

}

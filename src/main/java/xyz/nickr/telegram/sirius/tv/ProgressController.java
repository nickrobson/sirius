package xyz.nickr.telegram.sirius.tv;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.user.User;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
public class ProgressController {

    public Map<String, String> getProgress(User user) {
        return getProgress(user.getId());
    }

    public Map<String, String> getProgress(long userId) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("progress");

        Map<String, String> progress = new HashMap<>();
        try (MongoCursor<Document> cursor = collection.find(Filters.eq("user", userId)).projection(Projections.include("id", "episode")).iterator()) {
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
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("progress");

        Document document = collection.find(Filters.and(Filters.eq("id", id), Filters.eq("user", user.getId()))).projection(Projections.include("episode")).first();

        return document != null ? document.getString("episode") : null;
    }

    public void setProgress(User user, String id, String progress) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("progress");

        Document doc = new Document()
                .append("id", id)
                .append("user", user.getId())
                .append("episode", progress);

        collection.updateOne(Filters.and(Filters.eq("id", id), Filters.eq("user", user.getId())), new Document("$set", doc), new UpdateOptions().upsert(true));
    }

    public boolean removeProgress(User user, String id) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("progress");

        DeleteResult res = collection.deleteOne(Filters.and(Filters.eq("user", user.getId()), Filters.eq("id", id)));

        return res.getDeletedCount() > 0;
    }

}

package xyz.nickr.telegram.omnibot.storage;

import com.mongodb.client.MongoCollection;
import java.util.List;
import java.util.function.BiPredicate;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import xyz.nickr.telegram.omnibot.OmniBot;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
public class MongoPermissionPredicate implements BiPredicate<Message, String> {

    private MongoCollection<Document> collection;

    public MongoPermissionPredicate() {
        collection = OmniBot.getMongoController().getCollection("permissions");
    }

    @Override
    public boolean test(Message message, String permission) {
        Document document = collection.find(eq("user", message.getSender().getId())).first();
        if (document == null)
            return false;
        List<String> permissions = (List<String>) document.get("permissions");
        return permissions != null && permissions.contains(permission);
    }

}

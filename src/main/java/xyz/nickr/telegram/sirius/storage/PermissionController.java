package xyz.nickr.telegram.sirius.storage;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.user.User;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
public class PermissionController {

    public List<String> getPermissions(User user) {
        return getPermissions(user.getId());
    }

    public List<String> getPermissions(long user) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(Filters.eq("user", user)).projection(Projections.include("permissions")).first();
        if (document == null)
            return new LinkedList<>();
        List<String> permissions = (List<String>) document.get("permissions");
        return (permissions != null) ? new LinkedList<>(permissions) : null;
    }

    public boolean hasPermission(User user, String permission) {
        return getPermissions(user).contains(permission.toLowerCase(Locale.US));
    }

    public boolean hasPermission(long user, String permission) {
        return getPermissions(user).contains(permission.toLowerCase(Locale.US));
    }

    public void addPermission(User user, String permission) {
        addPermission(user.getId(), permission);
    }

    public void addPermission(long user, String permission) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(Filters.eq("user", user)).projection(Projections.include("permissions")).first();
        if (document == null) {
            document = new Document("user", user).append("permissions", Collections.singletonList(permission.toLowerCase(Locale.US)));
            collection.insertOne(document);
        } else {
            collection.updateOne(Filters.eq("user", user), new Document("$addToSet", new Document("permissions", permission.toLowerCase(Locale.US))));
        }
    }

    public void removePermission(User user, String permission) {
        removePermission(user.getId(), permission);
    }

    public void removePermission(long user, String permission) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(Filters.eq("user", user)).projection(Projections.include("user")).first();
        if (document != null) {
            collection.updateOne(Filters.eq("user", user), new Document("$pull", new Document("permissions", permission)));
        }
    }

}

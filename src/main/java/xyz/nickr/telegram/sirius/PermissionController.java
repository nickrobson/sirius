package xyz.nickr.telegram.sirius;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.bson.Document;
import pro.zackpollard.telegrambot.api.user.User;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
public class PermissionController {

    public List<String> getPermissions(User user) {
        return getPermissions(user.getId());
    }

    public List<String> getPermissions(long user) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(eq("user", user)).projection(Projections.include("permissions")).first();
        if (document == null)
            return new LinkedList<>();
        List<String> permissions = (List<String>) document.get("permissions");
        return permissions != null ? new LinkedList<>(permissions) : null;
    }

    public boolean hasPermission(User user, String permission) {
        return getPermissions(user).contains(permission.toLowerCase());
    }

    public boolean hasPermission(long user, String permission) {
        return getPermissions(user).contains(permission.toLowerCase());
    }

    public void addPermission(User user, String permission) {
        addPermission(user.getId(), permission);
    }

    public void addPermission(long user, String permission) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(eq("user", user)).projection(Projections.include("permissions")).first();
        if (document == null) {
            document = new Document("user", user).append("permissions", Collections.singletonList(permission.toLowerCase()));
            collection.insertOne(document);
        } else {
            collection.updateOne(eq("user", user), new Document("$push", new Document("permissions", permission.toLowerCase())));
        }
    }

    public void removePermission(User user, String permission) {
        removePermission(user.getId(), permission);
    }

    public void removePermission(long user, String permission) {
        MongoCollection<Document> collection = Sirius.getMongoController().getCollection("permissions");
        Document document = collection.find(eq("user", user)).projection(Projections.include("permissions")).first();
        if (document != null) {
            List<String> permissions = new LinkedList<>((List<String>) document.get("permissions"));
            permissions.remove(permission.toLowerCase());
            collection.updateOne(eq("user", user), new Document("$set", new Document("permissions", permissions)));
        }
    }

}

package xyz.nickr.telegram.omnibot.storage;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;

/**
 * @author Nick Robson
 */
public class MongoController {

    @Getter private MongoClient client;
    @Getter private MongoDatabase database;

    public MongoController() {
        this.client = new MongoClient();
        this.database = client.getDatabase("omnibot");
    }

    public MongoCollection<Document> getCollection(String name) {
        return this.database.getCollection(name);
    }

}

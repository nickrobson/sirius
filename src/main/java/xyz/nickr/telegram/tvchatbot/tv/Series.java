package xyz.nickr.telegram.tvchatbot.tv;

import com.mongodb.client.MongoCollection;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bson.Document;
import xyz.nickr.jomdb.model.SeasonResult;
import xyz.nickr.jomdb.model.TitleResult;
import xyz.nickr.telegram.tvchatbot.TvChatBot;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Nick Robson
 */
@Getter
public class Series {

    private String id;
    private String name;
    private Season[] seasons;

    public Series(String id, String name, Season[] seasons) {
        System.out.println("Invoked new Series(" + id + ", " + name + ", [..seasons..])");
        this.id = id;
        this.name = name;
        this.seasons = seasons;

        TvChatBot.getExecutor().submit(this::update);
    }

    public Series(String id) {
        System.out.println("Invoked new Series(" + id + ")");
        this.id = id;
        update();
    }

    public Document toDocument() {
        return new Document()
                .append("id", id)
                .append("name", name)
                .append("schema", 1)
                .append("seasons", Arrays.stream(this.seasons)
                        .map(Season::toDocument)
                        .collect(Collectors.toList()));
    }

    public void update() {
        TitleResult titleResult = TvChatBot.getOmdb().titleById(id);

        if (!"series".equals(titleResult.getType()))
            throw new IllegalArgumentException(id + " is a " + titleResult.getType() + " not a series!");

        this.name = titleResult.getTitle();
        this.seasons = new Season[titleResult.getTotalSeasons()];
        int i = 0;
        for (SeasonResult seasonResult : titleResult) {
            this.seasons[i++] = new Season(seasonResult);
        }

        TvChatBot.getExecutor().submit(() -> {
            MongoCollection<Document> collection = TvChatBot.getMongoController().getCollection("shows");

            Document document = this.toDocument();
            Document existing = collection.find(eq("id", id)).first();

            if (existing != null) {
                existing.remove("_id"); // the database one will (of course) have an id field; the generated one won't.
                document.append("links", existing.get("links")); // we want to keep the links array
            }

            if (existing == null) {
                System.out.println("Inserting database series model for id: " + id);
                collection.insertOne(document);
            } else if (!existing.equals(document)) {
                System.out.println("Updating database series model for id: " + id);
                System.out.println("Migrating from:");
                System.out.println(existing);
                System.out.println("to:");
                System.out.println(document);
                collection.replaceOne(eq("id", id), document);
            }
        });
    }

}

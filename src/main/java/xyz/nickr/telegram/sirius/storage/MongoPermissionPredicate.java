package xyz.nickr.telegram.sirius.storage;

import java.util.function.BiPredicate;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import xyz.nickr.telegram.sirius.Sirius;

/**
 * @author Nick Robson
 */
public class MongoPermissionPredicate implements BiPredicate<Message, String> {

    @Override
    public boolean test(Message message, String permission) {
        return Sirius.getPermissionController().hasPermission(message.getSender(), permission);
    }

}

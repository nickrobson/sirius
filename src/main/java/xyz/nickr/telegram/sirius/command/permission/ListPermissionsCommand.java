package xyz.nickr.telegram.sirius.command.permission;

import java.util.List;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class ListPermissionsCommand extends Command {

    public ListPermissionsCommand() {
        super("listperms");
        this.setHelp("lists your permissions");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        List<String> permissions = Sirius.getPermissionController().getPermissions(message.getSender());

        if (permissions.isEmpty()) {
            message.getChat().sendMessage(SendableTextMessage.markdown("You have no permissions.").replyTo(message).build());
        } else {
            PaginatedData paginatedData = new PaginatedData(permissions, 10);
            paginatedData.setParseMode(ParseMode.NONE);
            paginatedData.send(0, message);
        }
    }

}

package xyz.nickr.telegram.sirius.command.permission;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class AddPermissionCommand extends Command {

    public AddPermissionCommand() {
        super("addperm");
        this.setUsage("[user] [permission]");
        this.setHelp("adds a permission to a user");
        this.setPermission("admin.permission.add");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length < 2) {
            sendUsage(message);
        } else {
            Long target = Sirius.getBotInstance().getUserCache().getUserId(args[0]);
            if (target != null) {
                if (!Sirius.getPermissionController().hasPermission(target, args[1])) {
                    Sirius.getPermissionController().addPermission(target, args[1]);
                    message.getChat().sendMessage(SendableTextMessage.markdown("*" + escape(args[0]) + "*" + escape(" now has ") + "_" + escape(args[1]) + "_").replyTo(message).build());
                } else {
                    message.getChat().sendMessage(SendableTextMessage.markdown("*" + escape(args[0]) + "*" + escape(" already has ") + "_" + escape(args[1]) + "_").replyTo(message).build());
                }
            } else {
                message.getChat().sendMessage(SendableTextMessage.markdown(escape("I have not seen any user called '") + "*" + escape(args[0]) + "*" + escape("'!\nAsk them to send a command first!")).replyTo(message).build());
            }
        }
    }
}

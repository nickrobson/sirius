package xyz.nickr.telegram.sirius.command.permission;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class RemovePermissionCommand extends Command {

    public RemovePermissionCommand() {
        super("addperm");
        this.setUsage("[user] [permission]");
        this.setHelp("removes a permission from a user");
        this.setPermission("admin.permission.remove");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length < 2) {
            sendUsage(message);
        } else {
            Long target = Sirius.getBotInstance().getUserCache().getUserId(args[0]);
            if (target != null) {
                if (Sirius.getPermissionController().hasPermission(target, args[1])) {
                    Sirius.getPermissionController().removePermission(target, args[1]);
                    message.getChat().sendMessage(SendableTextMessage.markdown("*" + escape(args[0]) + "*" + escape(" no longer has ") + "_" + escape(args[1]) + "_").replyTo(message).build());
                } else {
                    message.getChat().sendMessage(SendableTextMessage.markdown("*" + escape(args[0]) + "*" + escape(" doesn't have ") + "_" + escape(args[1]) + "_").replyTo(message).build());
                }
            } else {
                message.getChat().sendMessage(SendableTextMessage.markdown(escape("I have not seen any user called '") + "*" + escape(args[0]) + "*" + escape("'!\nAsk them to send a command first!")).replyTo(message).build());
            }
        }
    }
}

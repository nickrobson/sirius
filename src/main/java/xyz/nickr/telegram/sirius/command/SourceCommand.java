package xyz.nickr.telegram.sirius.command;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class SourceCommand extends Command {

    public SourceCommand() {
        super("git", "source");
        this.setHelp("gets where you can find the bot's source");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        message.getChat().sendMessage(
                SendableTextMessage.markdown("You can find the source [here](http://github.com/nickrobson/sirius)")
                        .replyTo(message)
                        .build()
        );
    }

}

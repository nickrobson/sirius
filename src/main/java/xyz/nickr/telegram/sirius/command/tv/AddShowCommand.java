package xyz.nickr.telegram.sirius.command.tv;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class AddShowCommand extends Command {

    public AddShowCommand() {
        super("addshow");
        this.setHelp("adds a show to the list of tracked shows");
        this.setUsage("[imdb id] [name]");
        this.setPermission("show.add");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 2) {
            sendUsage(message);
        } else {
            Series series;
            try {
                series = Sirius.getSeriesController().getSeries(args[0], true);
            } catch (Exception ex) {
                message.getChat().sendMessage(SendableTextMessage.plain("That either isn't a show or it isn't a series!").replyTo(message).build());
                return;
            }
            Sirius.getSeriesController().addShow(series, args[1]);
            message.getChat().sendMessage(SendableTextMessage.plain("Successfully added " + escape(series.getName()) + "!").replyTo(message).build());
        }
    }

}
